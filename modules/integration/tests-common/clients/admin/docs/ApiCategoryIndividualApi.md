# ApiCategoryIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiCategoriesApiCategoryIdDelete**](ApiCategoryIndividualApi.md#apiCategoriesApiCategoryIdDelete) | **DELETE** /api-categories/{apiCategoryId} | Delete an API Category
[**apiCategoriesApiCategoryIdPut**](ApiCategoryIndividualApi.md#apiCategoriesApiCategoryIdPut) | **PUT** /api-categories/{apiCategoryId} | Update an API Category
[**apiCategoriesPost**](ApiCategoryIndividualApi.md#apiCategoriesPost) | **POST** /api-categories | Add a new API Category


<a name="apiCategoriesApiCategoryIdDelete"></a>
# **apiCategoriesApiCategoryIdDelete**
> apiCategoriesApiCategoryIdDelete(apiCategoryId, ifMatch, ifUnmodifiedSince)

Delete an API Category

Delete an API Category by API Category Id 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApiCategoryIndividualApi;


ApiCategoryIndividualApi apiInstance = new ApiCategoryIndividualApi();
String apiCategoryId = "apiCategoryId_example"; // String | API Category UUID 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.apiCategoriesApiCategoryIdDelete(apiCategoryId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCategoryIndividualApi#apiCategoriesApiCategoryIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiCategoryId** | **String**| API Category UUID  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiCategoriesApiCategoryIdPut"></a>
# **apiCategoriesApiCategoryIdPut**
> APICategoryDTO apiCategoriesApiCategoryIdPut(apiCategoryId, body)

Update an API Category

Update an API Category by category Id 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApiCategoryIndividualApi;


ApiCategoryIndividualApi apiInstance = new ApiCategoryIndividualApi();
String apiCategoryId = "apiCategoryId_example"; // String | API Category UUID 
APICategoryDTO body = new APICategoryDTO(); // APICategoryDTO | API Category object with updated information 
try {
    APICategoryDTO result = apiInstance.apiCategoriesApiCategoryIdPut(apiCategoryId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCategoryIndividualApi#apiCategoriesApiCategoryIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiCategoryId** | **String**| API Category UUID  |
 **body** | [**APICategoryDTO**](APICategoryDTO.md)| API Category object with updated information  |

### Return type

[**APICategoryDTO**](APICategoryDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiCategoriesPost"></a>
# **apiCategoriesPost**
> APICategoryDTO apiCategoriesPost(body)

Add a new API Category

Add a new API Category 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApiCategoryIndividualApi;


ApiCategoryIndividualApi apiInstance = new ApiCategoryIndividualApi();
APICategoryDTO body = new APICategoryDTO(); // APICategoryDTO | API Category object that should to be added 
try {
    APICategoryDTO result = apiInstance.apiCategoriesPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCategoryIndividualApi#apiCategoriesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**APICategoryDTO**](APICategoryDTO.md)| API Category object that should to be added  |

### Return type

[**APICategoryDTO**](APICategoryDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

