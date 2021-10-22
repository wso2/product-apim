# ResourceEndpointApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deleteResourceEndpoint**](ResourceEndpointApi.md#deleteResourceEndpoint) | **DELETE** /apis/{apiId}/resource-endpoints/{endpointId} | Delete Resource Endpoint
[**getResourceEndpoint**](ResourceEndpointApi.md#getResourceEndpoint) | **GET** /apis/{apiId}/resource-endpoints/{endpointId} | Get Resource Endpoint
[**updateResourceEndpoint**](ResourceEndpointApi.md#updateResourceEndpoint) | **PUT** /apis/{apiId}/resource-endpoints/{endpointId} | Update Resource Endpoint


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
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointApi apiInstance = new ResourceEndpointApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    try {
      apiInstance.deleteResourceEndpoint(apiId, endpointId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointApi#deleteResourceEndpoint");
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
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointApi apiInstance = new ResourceEndpointApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    try {
      ResourceEndpointDTO result = apiInstance.getResourceEndpoint(apiId, endpointId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointApi#getResourceEndpoint");
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
import org.wso2.am.integration.clients.publisher.api.v1.ResourceEndpointApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ResourceEndpointApi apiInstance = new ResourceEndpointApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    ResourceEndpointDTO resourceEndpointDTO = new ResourceEndpointDTO(); // ResourceEndpointDTO | Resource Endpoint object with updated details
    try {
      ResourceEndpointDTO result = apiInstance.updateResourceEndpoint(apiId, endpointId, resourceEndpointDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceEndpointApi#updateResourceEndpoint");
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

