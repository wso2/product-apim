# ExternalStoresApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllExternalStores**](ExternalStoresApi.md#getAllExternalStores) | **GET** /external-stores | Retrieve External Stores List to Publish an API
[**getAllPublishedExternalStoresByAPI**](ExternalStoresApi.md#getAllPublishedExternalStoresByAPI) | **GET** /apis/{apiId}/external-stores | Get the List of External Stores to which an API is Published
[**publishAPIToExternalStores**](ExternalStoresApi.md#publishAPIToExternalStores) | **POST** /apis/{apiId}/publish-to-external-stores | Publish an API to External Stores


<a name="getAllExternalStores"></a>
# **getAllExternalStores**
> ExternalStoreDTO getAllExternalStores()

Retrieve External Stores List to Publish an API

Retrieve external stores list configured to publish an API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ExternalStoresApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ExternalStoresApi apiInstance = new ExternalStoresApi(defaultClient);
    try {
      ExternalStoreDTO result = apiInstance.getAllExternalStores();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ExternalStoresApi#getAllExternalStores");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ExternalStoreDTO**](ExternalStoreDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. External Stores list returned  |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAllPublishedExternalStoresByAPI"></a>
# **getAllPublishedExternalStoresByAPI**
> APIExternalStoreListDTO getAllPublishedExternalStoresByAPI(apiId, ifNoneMatch)

Get the List of External Stores to which an API is Published

This operation can be used to retrieve a list of external stores which an API is published to by providing the id of the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ExternalStoresApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ExternalStoresApi apiInstance = new ExternalStoresApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      APIExternalStoreListDTO result = apiInstance.getAllPublishedExternalStoresByAPI(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ExternalStoresApi#getAllPublishedExternalStoresByAPI");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIExternalStoreListDTO**](APIExternalStoreListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. External Store list is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="publishAPIToExternalStores"></a>
# **publishAPIToExternalStores**
> APIExternalStoreListDTO publishAPIToExternalStores(apiId, externalStoreIds, ifMatch)

Publish an API to External Stores

This operation can be used to publish an API to a list of external stores. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ExternalStoresApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ExternalStoresApi apiInstance = new ExternalStoresApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String externalStoreIds = "externalStoreIds_example"; // String | External Store Ids of stores which the API needs to be published or updated.
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIExternalStoreListDTO result = apiInstance.publishAPIToExternalStores(apiId, externalStoreIds, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ExternalStoresApi#publishAPIToExternalStores");
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
 **externalStoreIds** | **String**| External Store Ids of stores which the API needs to be published or updated. | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIExternalStoreListDTO**](APIExternalStoreListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API was successfully published to all the selected external stores.  |  * ETag - Entity Tag of the blocked subscription. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the subscription has been blocked. Used by caches, or in conditional requests (Will be supported in future).  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

