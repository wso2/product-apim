# ApiCategoryCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiCategoriesGet**](ApiCategoryCollectionApi.md#apiCategoriesGet) | **GET** /api-categories | Get all API categories


<a name="apiCategoriesGet"></a>
# **apiCategoriesGet**
> APICategoryListDTO apiCategoriesGet()

Get all API categories

Get all API categories 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiCategoryCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiCategoryCollectionApi apiInstance = new ApiCategoryCollectionApi();
try {
    APICategoryListDTO result = apiInstance.apiCategoriesGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCategoryCollectionApi#apiCategoriesGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**APICategoryListDTO**](APICategoryListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

