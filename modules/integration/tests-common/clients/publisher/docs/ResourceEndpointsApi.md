# ResourceEndpointsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addResourceEndpoint**](ResourceEndpointsApi.md#addResourceEndpoint) | **POST** /apis/{apiId}/resource-endpoints | Add Resource Endpoint
[**deleteResourceEndpoint**](ResourceEndpointsApi.md#deleteResourceEndpoint) | **DELETE** /apis/{apiId}/resource-endpoints/{endpointId} | Delete Resource Endpoint
[**getResourceEndpoint**](ResourceEndpointsApi.md#getResourceEndpoint) | **GET** /apis/{apiId}/resource-endpoints/{endpointId} | Get Resource Endpoint
[**getResourceEndpoints**](ResourceEndpointsApi.md#getResourceEndpoints) | **GET** /apis/{apiId}/resource-endpoints | Get All Available Resource Endpoints of an API
[**updateResourceEndpoint**](ResourceEndpointsApi.md#updateResourceEndpoint) | **PUT** /apis/{apiId}/resource-endpoints/{endpointId} | Update Resource Endpoint


<a name="addResourceEndpoint"></a>
# **addResourceEndpoint**
> ResourceEndpointDTO addResourceEndpoint(apiId, resourceEndpointDTO)

Add Resource Endpoint

This operation can be used to add resource endpoints for an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointsApi apiInstance = new ResourceEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    ResourceEndpointDTO resourceEndpointDTO = new ResourceEndpointDTO(); // ResourceEndpointDTO | Resource Endpoint Object that needs to be added
    try {
      ResourceEndpointDTO result = apiInstance.addResourceEndpoint(apiId, resourceEndpointDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointsApi#addResourceEndpoint");
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
 **resourceEndpointDTO** | [**ResourceEndpointDTO**](ResourceEndpointDTO.md)| Resource Endpoint Object that needs to be added |

### Return type

[**ResourceEndpointDTO**](ResourceEndpointDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created Resource Endpoint object in the body.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will d be supported in future).  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="deleteResourceEndpoint"></a>
# **deleteResourceEndpoint**
> deleteResourceEndpoint(apiId, endpointId)

Delete Resource Endpoint

This operation can be used to delete a resource endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointsApi apiInstance = new ResourceEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    try {
      apiInstance.deleteResourceEndpoint(apiId, endpointId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointsApi#deleteResourceEndpoint");
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
 **endpointId** | **String**| **Endpoint ID** consisting of the **UUID** of the Endpoint**.  |

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
**200** | OK. Updated Resource Endpoints is returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getResourceEndpoint"></a>
# **getResourceEndpoint**
> ResourceEndpointDTO getResourceEndpoint(apiId, endpointId)

Get Resource Endpoint

This operation can be used to get a resource endpoint by id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointsApi apiInstance = new ResourceEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    try {
      ResourceEndpointDTO result = apiInstance.getResourceEndpoint(apiId, endpointId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointsApi#getResourceEndpoint");
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
 **endpointId** | **String**| **Endpoint ID** consisting of the **UUID** of the Endpoint**.  |

### Return type

[**ResourceEndpointDTO**](ResourceEndpointDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Resource Endpoints is returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getResourceEndpoints"></a>
# **getResourceEndpoints**
> ResourceEndpointListDTO getResourceEndpoints(apiId, limit, offset)

Get All Available Resource Endpoints of an API

This operation can be used to get all the available resource endpoints of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointsApi apiInstance = new ResourceEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      ResourceEndpointListDTO result = apiInstance.getResourceEndpoints(apiId, limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointsApi#getResourceEndpoints");
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

### Return type

[**ResourceEndpointListDTO**](ResourceEndpointListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Resource Endpoints list is returned.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |

<a name="updateResourceEndpoint"></a>
# **updateResourceEndpoint**
> ResourceEndpointDTO updateResourceEndpoint(apiId, endpointId, resourceEndpointDTO)

Update Resource Endpoint

This operation can be used to update a resource endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointsApi apiInstance = new ResourceEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    ResourceEndpointDTO resourceEndpointDTO = new ResourceEndpointDTO(); // ResourceEndpointDTO | Resource Endpoint object with updated details
    try {
      ResourceEndpointDTO result = apiInstance.updateResourceEndpoint(apiId, endpointId, resourceEndpointDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointsApi#updateResourceEndpoint");
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
 **endpointId** | **String**| **Endpoint ID** consisting of the **UUID** of the Endpoint**.  |
 **resourceEndpointDTO** | [**ResourceEndpointDTO**](ResourceEndpointDTO.md)| Resource Endpoint object with updated details | [optional]

### Return type

[**ResourceEndpointDTO**](ResourceEndpointDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Updated Resource Endpoints is returned.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

