# ApiMonetizationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdMonetizationGet**](ApiMonetizationApi.md#apisApiIdMonetizationGet) | **GET** /apis/{apiId}/monetization | Get monetization status for each tier in a given API
[**apisApiIdMonetizePost**](ApiMonetizationApi.md#apisApiIdMonetizePost) | **POST** /apis/{apiId}/monetize | Configure monetization for a given API
[**apisApiIdRevenueGet**](ApiMonetizationApi.md#apisApiIdRevenueGet) | **GET** /apis/{apiId}/revenue | Get total revenue details of a given monetized API with meterd billing.
[**subscriptionsSubscriptionIdUsageGet**](ApiMonetizationApi.md#subscriptionsSubscriptionIdUsageGet) | **GET** /subscriptions/{subscriptionId}/usage | Get details of a pending invoice for a monetized subscription with meterd billing.


<a name="apisApiIdMonetizationGet"></a>
# **apisApiIdMonetizationGet**
> apisApiIdMonetizationGet(apiId)

Get monetization status for each tier in a given API

This operation can be used to get monetization status for each tier in a given API 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMonetizationApi apiInstance = new ApiMonetizationApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    apiInstance.apisApiIdMonetizationGet(apiId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMonetizationApi#apisApiIdMonetizationGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMonetizePost"></a>
# **apisApiIdMonetizePost**
> apisApiIdMonetizePost(apiId, body)

Configure monetization for a given API

This operation can be used to configure monetization for a given API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMonetizationApi apiInstance = new ApiMonetizationApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
APIMonetizationInfoDTO body = new APIMonetizationInfoDTO(); // APIMonetizationInfoDTO | Monetization data object 
try {
    apiInstance.apisApiIdMonetizePost(apiId, body);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMonetizationApi#apisApiIdMonetizePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**APIMonetizationInfoDTO**](APIMonetizationInfoDTO.md)| Monetization data object  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdRevenueGet"></a>
# **apisApiIdRevenueGet**
> APIRevenueDTO apisApiIdRevenueGet(apiId)

Get total revenue details of a given monetized API with meterd billing.

This operation can be used to get details of total revenue details of a given monetized API with meterd billing. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMonetizationApi apiInstance = new ApiMonetizationApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    APIRevenueDTO result = apiInstance.apisApiIdRevenueGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMonetizationApi#apisApiIdRevenueGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

[**APIRevenueDTO**](APIRevenueDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsSubscriptionIdUsageGet"></a>
# **subscriptionsSubscriptionIdUsageGet**
> APIMonetizationUsageDTO subscriptionsSubscriptionIdUsageGet(subscriptionId)

Get details of a pending invoice for a monetized subscription with meterd billing.

This operation can be used to get details of a pending invoice for a monetized subscription with meterd billing. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMonetizationApi apiInstance = new ApiMonetizationApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
try {
    APIMonetizationUsageDTO result = apiInstance.subscriptionsSubscriptionIdUsageGet(subscriptionId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMonetizationApi#subscriptionsSubscriptionIdUsageGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |

### Return type

[**APIMonetizationUsageDTO**](APIMonetizationUsageDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

