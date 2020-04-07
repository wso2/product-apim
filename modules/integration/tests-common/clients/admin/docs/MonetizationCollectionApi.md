# MonetizationCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**monetizationPublishUsagePost**](MonetizationCollectionApi.md#monetizationPublishUsagePost) | **POST** /monetization/publish-usage | Publish Usage Records
[**monetizationPublishUsageStatusGet**](MonetizationCollectionApi.md#monetizationPublishUsageStatusGet) | **GET** /monetization/publish-usage/status | Get the status of Monetization usage publisher


<a name="monetizationPublishUsagePost"></a>
# **monetizationPublishUsagePost**
> PublishStatusDTO monetizationPublishUsagePost()

Publish Usage Records

Publish Usage Records of Monetized APIs 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.MonetizationCollectionApi;


MonetizationCollectionApi apiInstance = new MonetizationCollectionApi();
try {
    PublishStatusDTO result = apiInstance.monetizationPublishUsagePost();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MonetizationCollectionApi#monetizationPublishUsagePost");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**PublishStatusDTO**](PublishStatusDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="monetizationPublishUsageStatusGet"></a>
# **monetizationPublishUsageStatusGet**
> MonetizationUsagePublishInfoDTO monetizationPublishUsageStatusGet()

Get the status of Monetization usage publisher

Get the status of Monetization usage publisher 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.MonetizationCollectionApi;


MonetizationCollectionApi apiInstance = new MonetizationCollectionApi();
try {
    MonetizationUsagePublishInfoDTO result = apiInstance.monetizationPublishUsageStatusGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MonetizationCollectionApi#monetizationPublishUsageStatusGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**MonetizationUsagePublishInfoDTO**](MonetizationUsagePublishInfoDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

