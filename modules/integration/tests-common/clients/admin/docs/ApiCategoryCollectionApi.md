# ApiCategoryCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

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
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApiCategoryCollectionApi;


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

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

