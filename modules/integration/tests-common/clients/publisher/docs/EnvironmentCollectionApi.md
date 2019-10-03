# EnvironmentCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**environmentsGet**](EnvironmentCollectionApi.md#environmentsGet) | **GET** /environments | Get all gateway environments


<a name="environmentsGet"></a>
# **environmentsGet**
> EnvironmentListDTO environmentsGet(apiId)

Get all gateway environments

This operation can be used to retrieve the list of gateway environments available. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.EnvironmentCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

EnvironmentCollectionApi apiInstance = new EnvironmentCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
try {
    EnvironmentListDTO result = apiInstance.environmentsGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling EnvironmentCollectionApi#environmentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |

### Return type

[**EnvironmentListDTO**](EnvironmentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

