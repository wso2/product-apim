# LabelCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**labelsGet**](LabelCollectionApi.md#labelsGet) | **GET** /labels | Get all registered Labels


<a name="labelsGet"></a>
# **labelsGet**
> LabelListDTO labelsGet()

Get all registered Labels

Get all registered Labels 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.LabelCollectionApi;


LabelCollectionApi apiInstance = new LabelCollectionApi();
try {
    LabelListDTO result = apiInstance.labelsGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabelCollectionApi#labelsGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**LabelListDTO**](LabelListDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

