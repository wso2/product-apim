# ApiProductsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiProductsApiProductIdDelete**](ApiProductsApi.md#apiProductsApiProductIdDelete) | **DELETE** /api-products/{apiProductId} | Delete an API Product
[**apiProductsApiProductIdGet**](ApiProductsApi.md#apiProductsApiProductIdGet) | **GET** /api-products/{apiProductId} | Get details of an API Product
[**apiProductsApiProductIdIsOutdatedGet**](ApiProductsApi.md#apiProductsApiProductIdIsOutdatedGet) | **GET** /api-products/{apiProductId}/is-outdated | Get if API Product is outdated
[**apiProductsApiProductIdPut**](ApiProductsApi.md#apiProductsApiProductIdPut) | **PUT** /api-products/{apiProductId} | Update an API product
[**apiProductsApiProductIdSwaggerGet**](ApiProductsApi.md#apiProductsApiProductIdSwaggerGet) | **GET** /api-products/{apiProductId}/swagger | Get swagger definition
[**apiProductsApiProductIdThumbnailGet**](ApiProductsApi.md#apiProductsApiProductIdThumbnailGet) | **GET** /api-products/{apiProductId}/thumbnail | Get thumbnail image
[**apiProductsApiProductIdThumbnailPut**](ApiProductsApi.md#apiProductsApiProductIdThumbnailPut) | **PUT** /api-products/{apiProductId}/thumbnail | Upload a thumbnail image
[**apiProductsGet**](ApiProductsApi.md#apiProductsGet) | **GET** /api-products | Retrieve/Search API Products 
[**apiProductsPost**](ApiProductsApi.md#apiProductsPost) | **POST** /api-products | Create a new API Product


<a name="apiProductsApiProductIdDelete"></a>
# **apiProductsApiProductIdDelete**
> apiProductsApiProductIdDelete(apiProductId, ifMatch)

Delete an API Product

This operation can be used to delete an existing API Product proving the Id of the API Product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apiProductsApiProductIdDelete(apiProductId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdDelete");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdGet"></a>
# **apiProductsApiProductIdGet**
> APIProductDTO apiProductsApiProductIdGet(apiProductId, accept, ifNoneMatch)

Get details of an API Product

Using this operation, you can retrieve complete details of a single API Product. You need to provide the Id of the API to retrive it. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    APIProductDTO result = apiInstance.apiProductsApiProductIdGet(apiProductId, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdIsOutdatedGet"></a>
# **apiProductsApiProductIdIsOutdatedGet**
> APIProductOutdatedStatusDTO apiProductsApiProductIdIsOutdatedGet(apiProductId, accept, ifNoneMatch)

Get if API Product is outdated

This operation can be used to retrieve the status indicating if an API Product is outdated due to updating of dependent APIs 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    APIProductOutdatedStatusDTO result = apiInstance.apiProductsApiProductIdIsOutdatedGet(apiProductId, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdIsOutdatedGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIProductOutdatedStatusDTO**](APIProductOutdatedStatusDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdPut"></a>
# **apiProductsApiProductIdPut**
> APIProductDTO apiProductsApiProductIdPut(apiProductId, body, ifMatch)

Update an API product

This operation can be used to update an existing API product. But the properties &#x60;name&#x60;, &#x60;provider&#x60; 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
APIProductDTO body = new APIProductDTO(); // APIProductDTO | API object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIProductDTO result = apiInstance.apiProductsApiProductIdPut(apiProductId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **body** | [**APIProductDTO**](APIProductDTO.md)| API object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdSwaggerGet"></a>
# **apiProductsApiProductIdSwaggerGet**
> apiProductsApiProductIdSwaggerGet(apiProductId, accept, ifNoneMatch)

Get swagger definition

This operation can be used to retrieve the swagger definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apiProductsApiProductIdSwaggerGet(apiProductId, accept, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdSwaggerGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdThumbnailGet"></a>
# **apiProductsApiProductIdThumbnailGet**
> apiProductsApiProductIdThumbnailGet(apiProductId, accept, ifNoneMatch)

Get thumbnail image

This operation can be used to download a thumbnail image of an API product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apiProductsApiProductIdThumbnailGet(apiProductId, accept, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdThumbnailGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdThumbnailPut"></a>
# **apiProductsApiProductIdThumbnailPut**
> FileInfoDTO apiProductsApiProductIdThumbnailPut(apiProductId, file, ifMatch)

Upload a thumbnail image

This operation can be used to upload a thumbnail image of an API Product. The thumbnail to be uploaded should be given as a form data parameter &#x60;file&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
File file = new File("/path/to/file.txt"); // File | Image to upload
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    FileInfoDTO result = apiInstance.apiProductsApiProductIdThumbnailPut(apiProductId, file, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdThumbnailPut");
    e.printStackTrace();
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

<a name="apiProductsGet"></a>
# **apiProductsGet**
> APIProductListDTO apiProductsGet(limit, offset, query, accept, ifNoneMatch)

Retrieve/Search API Products 

This operation provides you a list of available API Products qualifying under a given search condition.  Each retrieved API Product is represented with a minimal amount of attributes. If you want to get complete details of an API Product, you need to use **Get details of an API Product** operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    APIProductListDTO result = apiInstance.apiProductsGet(limit, offset, query, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**|  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIProductListDTO**](APIProductListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsPost"></a>
# **apiProductsPost**
> APIProductDTO apiProductsPost(body)

Create a new API Product

This operation can be used to create a new API Product specifying the details of the API Product in the payload. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
APIProductDTO body = new APIProductDTO(); // APIProductDTO | API object that needs to be added 
try {
    APIProductDTO result = apiInstance.apiProductsPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**APIProductDTO**](APIProductDTO.md)| API object that needs to be added  |

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

