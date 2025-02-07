# ApiProductsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAPIProduct**](ApiProductsApi.md#createAPIProduct) | **POST** /api-products | Create a New API Product
[**createNewAPIProductVersion**](ApiProductsApi.md#createNewAPIProductVersion) | **POST** /api-products/copy-api-products | Create a New API Product Version
[**deleteAPIProduct**](ApiProductsApi.md#deleteAPIProduct) | **DELETE** /api-products/{apiProductId} | Delete an API Product
[**getAPIProduct**](ApiProductsApi.md#getAPIProduct) | **GET** /api-products/{apiProductId} | Get Details of an API Product
[**getAPIProductSwagger**](ApiProductsApi.md#getAPIProductSwagger) | **GET** /api-products/{apiProductId}/swagger | Get Swagger Definition
[**getAPIProductThumbnail**](ApiProductsApi.md#getAPIProductThumbnail) | **GET** /api-products/{apiProductId}/thumbnail | Get Thumbnail Image
[**getAllAPIProducts**](ApiProductsApi.md#getAllAPIProducts) | **GET** /api-products | Retrieve/Search API Products 
[**getIsAPIProductOutdated**](ApiProductsApi.md#getIsAPIProductOutdated) | **GET** /api-products/{apiProductId}/is-outdated | Check Whether API Product is Outdated
[**updateAPIProduct**](ApiProductsApi.md#updateAPIProduct) | **PUT** /api-products/{apiProductId} | Update an API Product
[**updateAPIProductThumbnail**](ApiProductsApi.md#updateAPIProductThumbnail) | **PUT** /api-products/{apiProductId}/thumbnail | Upload a Thumbnail Image


<a name="createAPIProduct"></a>
# **createAPIProduct**
> APIProductDTO createAPIProduct(apIProductDTO)

Create a New API Product

This operation can be used to create a new API Product specifying the details of the API Product in the payload. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    APIProductDTO apIProductDTO = new APIProductDTO(); // APIProductDTO | API object that needs to be added
    try {
      APIProductDTO result = apiInstance.createAPIProduct(apIProductDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#createAPIProduct");
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
 **apIProductDTO** | [**APIProductDTO**](APIProductDTO.md)| API object that needs to be added |

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | &#39;Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.&#39;  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="createNewAPIProductVersion"></a>
# **createNewAPIProductVersion**
> APIProductDTO createNewAPIProductVersion(newVersion, apiProductId, defaultVersion)

Create a New API Product Version

This operation can be used to create a new version of an existing API Products. The new version is specified as &#x60;newVersion&#x60; query parameter. New API Product will be in &#x60;CREATED&#x60; state. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String newVersion = "newVersion_example"; // String | Version of the new API Product.
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. The combination of the provider, name and the version of the API Product is also accepted as a valid API Product ID. Should be formatted as **provider-name-version**. 
    Boolean defaultVersion = false; // Boolean | Specifies whether new API Product should be added as default version.
    try {
      APIProductDTO result = apiInstance.createNewAPIProductVersion(newVersion, apiProductId, defaultVersion);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#createNewAPIProductVersion");
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
 **newVersion** | **String**| Version of the new API Product. |
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. The combination of the provider, name and the version of the API Product is also accepted as a valid API Product ID. Should be formatted as **provider-name-version**.  |
 **defaultVersion** | **Boolean**| Specifies whether new API Product should be added as default version. | [optional] [default to false]

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created API Product as entity in the body. Location header contains URL of newly created API Product.  |  * Location - The URL of the newly created API Product.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="deleteAPIProduct"></a>
# **deleteAPIProduct**
> deleteAPIProduct(apiProductId, ifMatch)

Delete an API Product

This operation can be used to delete an existing API Product proving the Id of the API Product. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteAPIProduct(apiProductId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#deleteAPIProduct");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
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
**200** | OK. Resource successfully deleted.  |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getAPIProduct"></a>
# **getAPIProduct**
> APIProductDTO getAPIProduct(apiProductId, accept, ifNoneMatch)

Get Details of an API Product

Using this operation, you can retrieve complete details of a single API Product. You need to provide the Id of the API to retrive it. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      APIProductDTO result = apiInstance.getAPIProduct(apiProductId, accept, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#getAPIProduct");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested API Product is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIProductSwagger"></a>
# **getAPIProductSwagger**
> String getAPIProductSwagger(apiProductId, accept, ifNoneMatch)

Get Swagger Definition

This operation can be used to retrieve the swagger definition of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      String result = apiInstance.getAPIProductSwagger(apiProductId, accept, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#getAPIProductSwagger");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested swagger document of the API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIProductThumbnail"></a>
# **getAPIProductThumbnail**
> getAPIProductThumbnail(apiProductId, accept, ifNoneMatch)

Get Thumbnail Image

This operation can be used to download a thumbnail image of an API product. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      apiInstance.getAPIProductThumbnail(apiProductId, accept, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#getAPIProductThumbnail");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

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
**200** | OK. Thumbnail image returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAllAPIProducts"></a>
# **getAllAPIProducts**
> APIProductListDTO getAllAPIProducts(limit, offset, query, accept, ifNoneMatch)

Retrieve/Search API Products 

This operation provides you a list of available API Products qualifying under a given search condition.  Each retrieved API Product is represented with a minimal amount of attributes. If you want to get complete details of an API Product, you need to use **Get details of an API Product** operation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String query = "query_example"; // String | 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      APIProductListDTO result = apiInstance.getAllAPIProducts(limit, offset, query, accept, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#getAllAPIProducts");
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
 **query** | **String**|  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIProductListDTO**](APIProductListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of qualifying API Products is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getIsAPIProductOutdated"></a>
# **getIsAPIProductOutdated**
> APIProductOutdatedStatusDTO getIsAPIProductOutdated(apiProductId, accept, ifNoneMatch)

Check Whether API Product is Outdated

This operation can be used to retrieve the status indicating if an API Product is outdated due to updating of dependent APIs (This resource is not supported at the moment) 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      APIProductOutdatedStatusDTO result = apiInstance.getIsAPIProductOutdated(apiProductId, accept, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#getIsAPIProductOutdated");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIProductOutdatedStatusDTO**](APIProductOutdatedStatusDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested swagger document of the API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="updateAPIProduct"></a>
# **updateAPIProduct**
> APIProductDTO updateAPIProduct(apiProductId, apIProductDTO, ifMatch)

Update an API Product

This operation can be used to update an existing API product. But the properties &#x60;name&#x60;, &#x60;provider&#x60; and &#x60;version&#x60; cannot be changed. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    APIProductDTO apIProductDTO = new APIProductDTO(); // APIProductDTO | API object that needs to be added
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIProductDTO result = apiInstance.updateAPIProduct(apiProductId, apIProductDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#updateAPIProduct");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **apIProductDTO** | [**APIProductDTO**](APIProductDTO.md)| API object that needs to be added |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API product object  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="updateAPIProductThumbnail"></a>
# **updateAPIProductThumbnail**
> FileInfoDTO updateAPIProductThumbnail(apiProductId, file, ifMatch)

Upload a Thumbnail Image

This operation can be used to upload a thumbnail image of an API Product. The thumbnail to be uploaded should be given as a form data parameter &#x60;file&#x60;. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductsApi apiInstance = new ApiProductsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    File file = new File("/path/to/file"); // File | Image to upload
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      FileInfoDTO result = apiInstance.updateAPIProductThumbnail(apiProductId, file, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductsApi#updateAPIProductThumbnail");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **file** | **File**| Image to upload |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**FileInfoDTO**](FileInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Image updated  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the uploaded thumbnail image of the API Product.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

