# RecommendationsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**recommendationsGet**](RecommendationsApi.md#recommendationsGet) | **GET** /recommendations | Give API recommendations for a user


<a name="recommendationsGet"></a>
# **recommendationsGet**
> RecommendationsDTO recommendationsGet()

Give API recommendations for a user

This API can be used to get recommended APIs for a user who logs into the API store

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RecommendationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RecommendationsApi apiInstance = new RecommendationsApi();
try {
    RecommendationsDTO result = apiInstance.recommendationsGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RecommendationsApi#recommendationsGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**RecommendationsDTO**](RecommendationsDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

