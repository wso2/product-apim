# EnvironmentCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**environmentsGet**](EnvironmentCollectionApi.md#environmentsGet) | **GET** /environments | Get all gateway environments


<a name="environmentsGet"></a>
# **environmentsGet**
> EnvironmentList environmentsGet(apiId)

Get all gateway environments

This operation can be used to retrieve the list of gateway environments available. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.EnvironmentCollectionApi;


EnvironmentCollectionApi apiInstance = new EnvironmentCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
try {
    EnvironmentList result = apiInstance.environmentsGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling EnvironmentCollectionApi#environmentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |

### Return type

[**EnvironmentList**](EnvironmentList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

