# ExternalStoresApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllExternalStores**](ExternalStoresApi.md#getAllExternalStores) | **GET** /external-stores | Retrieve external store list to publish an API
[**getAllPublishedExternalStoresByAPI**](ExternalStoresApi.md#getAllPublishedExternalStoresByAPI) | **GET** /apis/{apiId}/external-stores | Get the list of external stores which an API is published to
[**publishAPIToExternalStores**](ExternalStoresApi.md#publishAPIToExternalStores) | **POST** /apis/{apiId}/publish-to-external-stores | Publish an API to external stores


<a name="getAllExternalStores"></a>
# **getAllExternalStores**
> ExternalStoreDTO getAllExternalStores()

Retrieve external store list to publish an API

Retrieve external stores list configured to publish an API 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ExternalStoresApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ExternalStoresApi apiInstance = new ExternalStoresApi();
try {
    ExternalStoreDTO result = apiInstance.getAllExternalStores();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ExternalStoresApi#getAllExternalStores");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ExternalStoreDTO**](ExternalStoreDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getAllPublishedExternalStoresByAPI"></a>
# **getAllPublishedExternalStoresByAPI**
> APIExternalStoreListDTO getAllPublishedExternalStoresByAPI(apiId, ifNoneMatch)

Get the list of external stores which an API is published to

This operation can be used to retrieve a list of external stores which an API is published to by providing the id of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ExternalStoresApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ExternalStoresApi apiInstance = new ExternalStoresApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    APIExternalStoreListDTO result = apiInstance.getAllPublishedExternalStoresByAPI(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ExternalStoresApi#getAllPublishedExternalStoresByAPI");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="publishAPIToExternalStores"></a>
# **publishAPIToExternalStores**
> APIExternalStoreListDTO publishAPIToExternalStores(apiId, externalStoreIds, ifMatch)

Publish an API to external stores

This operation can be used to publish an API to a list of external stores. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ExternalStoresApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ExternalStoresApi apiInstance = new ExternalStoresApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String externalStoreIds = "externalStoreIds_example"; // String | External Store Ids of stores which the API needs to be published or updated.
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIExternalStoreListDTO result = apiInstance.publishAPIToExternalStores(apiId, externalStoreIds, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ExternalStoresApi#publishAPIToExternalStores");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **externalStoreIds** | **String**| External Store Ids of stores which the API needs to be published or updated. |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIExternalStoreListDTO**](APIExternalStoreListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

