# LabelCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

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
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.LabelCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

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

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

