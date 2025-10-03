# ApiEndpointsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addApiEndpoint**](ApiEndpointsApi.md#addApiEndpoint) | **POST** /apis/{apiId}/endpoints | Add an Endpoint
[**deleteApiEndpoint**](ApiEndpointsApi.md#deleteApiEndpoint) | **DELETE** /apis/{apiId}/endpoints/{endpointId} | Delete an Endpoint
[**getApiEndpoint**](ApiEndpointsApi.md#getApiEndpoint) | **GET** /apis/{apiId}/endpoints/{endpointId} | Get an Endpoint
[**getApiEndpoints**](ApiEndpointsApi.md#getApiEndpoints) | **GET** /apis/{apiId}/endpoints | Get all API Endpoints
[**updateApiEndpoint**](ApiEndpointsApi.md#updateApiEndpoint) | **PUT** /apis/{apiId}/endpoints/{endpointId} | Update an Endpoint


<a name="addApiEndpoint"></a>
# **addApiEndpoint**
> APIEndpointDTO addApiEndpoint(apiId, apIEndpointDTO)

Add an Endpoint

This operation can be used to add an endpoint to an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiEndpointsApi apiInstance = new ApiEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIEndpointDTO apIEndpointDTO = new APIEndpointDTO(); // APIEndpointDTO | Endpoint object that needs to be added
    try {
      APIEndpointDTO result = apiInstance.addApiEndpoint(apiId, apIEndpointDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiEndpointsApi#addApiEndpoint");
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
 **apIEndpointDTO** | [**APIEndpointDTO**](APIEndpointDTO.md)| Endpoint object that needs to be added |

### Return type

[**APIEndpointDTO**](APIEndpointDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created API Endpoint object in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="deleteApiEndpoint"></a>
# **deleteApiEndpoint**
> deleteApiEndpoint(apiId, endpointId)

Delete an Endpoint

This operation can be used to delete a API endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiEndpointsApi apiInstance = new ApiEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    try {
      apiInstance.deleteApiEndpoint(apiId, endpointId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiEndpointsApi#deleteApiEndpoint");
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
**200** | OK. Endpoint deleted successfully.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getApiEndpoint"></a>
# **getApiEndpoint**
> APIEndpointDTO getApiEndpoint(apiId, endpointId)

Get an Endpoint

This operation can be used to get an endpoint of an API by UUID. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiEndpointsApi apiInstance = new ApiEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    try {
      APIEndpointDTO result = apiInstance.getApiEndpoint(apiId, endpointId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiEndpointsApi#getApiEndpoint");
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

[**APIEndpointDTO**](APIEndpointDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API Endpoint object is returned.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getApiEndpoints"></a>
# **getApiEndpoints**
> APIEndpointListDTO getApiEndpoints(apiId, limit, offset)

Get all API Endpoints

This operation can be used to get all the available endpoints of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiEndpointsApi apiInstance = new ApiEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      APIEndpointListDTO result = apiInstance.getApiEndpoints(apiId, limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiEndpointsApi#getApiEndpoints");
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

[**APIEndpointListDTO**](APIEndpointListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of API endpoints.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="updateApiEndpoint"></a>
# **updateApiEndpoint**
> APIEndpointDTO updateApiEndpoint(apiId, endpointId, apIEndpointDTO)

Update an Endpoint

This operation can be used to update a API endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiEndpointsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiEndpointsApi apiInstance = new ApiEndpointsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String endpointId = "endpointId_example"; // String | **Endpoint ID** consisting of the **UUID** of the Endpoint**. 
    APIEndpointDTO apIEndpointDTO = new APIEndpointDTO(); // APIEndpointDTO | API Endpoint object with updated details
    try {
      APIEndpointDTO result = apiInstance.updateApiEndpoint(apiId, endpointId, apIEndpointDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiEndpointsApi#updateApiEndpoint");
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
 **apIEndpointDTO** | [**APIEndpointDTO**](APIEndpointDTO.md)| API Endpoint object with updated details | [optional]

### Return type

[**APIEndpointDTO**](APIEndpointDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Updated API Endpoint is returned.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

