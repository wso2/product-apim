# RatingsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addMCPServerRating**](RatingsApi.md#addMCPServerRating) | **PUT** /mcp-servers/{mcpServerId}/user-rating | Add or Update Logged in User&#39;s Rating for a MCP Server
[**apisApiIdRatingsGet**](RatingsApi.md#apisApiIdRatingsGet) | **GET** /apis/{apiId}/ratings | Retrieve API Ratings
[**apisApiIdUserRatingDelete**](RatingsApi.md#apisApiIdUserRatingDelete) | **DELETE** /apis/{apiId}/user-rating | Delete User API Rating
[**apisApiIdUserRatingGet**](RatingsApi.md#apisApiIdUserRatingGet) | **GET** /apis/{apiId}/user-rating | Retrieve API Rating of User
[**apisApiIdUserRatingPut**](RatingsApi.md#apisApiIdUserRatingPut) | **PUT** /apis/{apiId}/user-rating | Add or Update Logged in User&#39;s Rating for an API
[**deleteMCPServerRating**](RatingsApi.md#deleteMCPServerRating) | **DELETE** /mcp-servers/{mcpServerId}/user-rating | Delete User MCP Server Rating
[**getMCPServerRating**](RatingsApi.md#getMCPServerRating) | **GET** /mcp-servers/{mcpServerId}/user-rating | Retrieve MCP Server Rating of User
[**getMCPServerRatings**](RatingsApi.md#getMCPServerRatings) | **GET** /mcp-servers/{mcpServerId}/ratings | Retrieve MCP Server Ratings


<a name="addMCPServerRating"></a>
# **addMCPServerRating**
> RatingDTO addMCPServerRating(mcpServerId, ratingDTO, xWSO2Tenant)

Add or Update Logged in User&#39;s Rating for a MCP Server

This operation can be used to add or update a MCP Server rating.  &#x60;X-WSO2-Tenant&#x60; header can be used to add or update the logged in user rating of a MCP Server that belongs to a  different tenant domain. If not specified super tenant will be used. If Authorization header is present in  the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    RatingDTO ratingDTO = new RatingDTO(); // RatingDTO | Rating object that should to be added 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      RatingDTO result = apiInstance.addMCPServerRating(mcpServerId, ratingDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#addMCPServerRating");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **ratingDTO** | [**RatingDTO**](RatingDTO.md)| Rating object that should to be added  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with the newly created or updated object as entity in the body.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="apisApiIdRatingsGet"></a>
# **apisApiIdRatingsGet**
> RatingListDTO apisApiIdRatingsGet(apiId, limit, offset, xWSO2Tenant)

Retrieve API Ratings

This operation can be used to retrieve the list of ratings of an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve ratings of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      RatingListDTO result = apiInstance.apisApiIdRatingsGet(apiId, limit, offset, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#apisApiIdRatingsGet");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**RatingListDTO**](RatingListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Rating list returned.  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="apisApiIdUserRatingDelete"></a>
# **apisApiIdUserRatingDelete**
> apisApiIdUserRatingDelete(apiId, xWSO2Tenant, ifMatch)

Delete User API Rating

This operation can be used to delete logged in user API rating.  &#x60;X-WSO2-Tenant&#x60; header can be used to delete the logged in user rating of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.apisApiIdUserRatingDelete(apiId, xWSO2Tenant, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#apisApiIdUserRatingDelete");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Resource successfully deleted.  |  -  |

<a name="apisApiIdUserRatingGet"></a>
# **apisApiIdUserRatingGet**
> RatingDTO apisApiIdUserRatingGet(apiId, xWSO2Tenant, ifNoneMatch)

Retrieve API Rating of User

This operation can be used to get the user rating of an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve the logged in user rating of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      RatingDTO result = apiInstance.apisApiIdUserRatingGet(apiId, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#apisApiIdUserRatingGet");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Rating returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client already has the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="apisApiIdUserRatingPut"></a>
# **apisApiIdUserRatingPut**
> RatingDTO apisApiIdUserRatingPut(apiId, ratingDTO, xWSO2Tenant)

Add or Update Logged in User&#39;s Rating for an API

This operation can be used to add or update an API rating.  &#x60;X-WSO2-Tenant&#x60; header can be used to add or update the logged in user rating of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    RatingDTO ratingDTO = new RatingDTO(); // RatingDTO | Rating object that should to be added 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      RatingDTO result = apiInstance.apisApiIdUserRatingPut(apiId, ratingDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#apisApiIdUserRatingPut");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ratingDTO** | [**RatingDTO**](RatingDTO.md)| Rating object that should to be added  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with the newly created or updated object as entity in the body.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="deleteMCPServerRating"></a>
# **deleteMCPServerRating**
> deleteMCPServerRating(mcpServerId, xWSO2Tenant, ifMatch)

Delete User MCP Server Rating

This operation can be used to delete logged in user MCP Server rating.  &#x60;X-WSO2-Tenant&#x60; header can be used to delete the logged in user rating of a MCP Server that belongs to a  different tenant domain. If not specified super tenant will be used. If Authorization header is present in the  request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteMCPServerRating(mcpServerId, xWSO2Tenant, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#deleteMCPServerRating");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Resource successfully deleted.  |  -  |

<a name="getMCPServerRating"></a>
# **getMCPServerRating**
> RatingDTO getMCPServerRating(mcpServerId, xWSO2Tenant, ifNoneMatch)

Retrieve MCP Server Rating of User

This operation can be used to get the user rating of a MCP Server.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve the logged in user rating of a MCP Server that belongs to a  different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      RatingDTO result = apiInstance.getMCPServerRating(mcpServerId, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#getMCPServerRating");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Rating returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client already has the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerRatings"></a>
# **getMCPServerRatings**
> RatingListDTO getMCPServerRatings(mcpServerId, limit, offset, xWSO2Tenant)

Retrieve MCP Server Ratings

This operation can be used to retrieve the list of ratings of a MCP Server.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve ratings of a MCP Server that belongs to a different tenant  domain. If not specified super tenant will be used. If Authorization header is present in the request, the  user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RatingsApi apiInstance = new RatingsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      RatingListDTO result = apiInstance.getMCPServerRatings(mcpServerId, limit, offset, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RatingsApi#getMCPServerRatings");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**RatingListDTO**](RatingListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Rating list returned.  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

