# ApiMonetizationApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsSubscriptionIdUsageGet**](ApiMonetizationApi.md#subscriptionsSubscriptionIdUsageGet) | **GET** /subscriptions/{subscriptionId}/usage | Get details of a pending invoice for a monetized subscription with metered billing.


<a name="subscriptionsSubscriptionIdUsageGet"></a>
# **subscriptionsSubscriptionIdUsageGet**
> APIMonetizationUsageDTO subscriptionsSubscriptionIdUsageGet(subscriptionId)

Get details of a pending invoice for a monetized subscription with metered billing.

This operation can be used to get details of a pending invoice for a monetized subscription with metered billing. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiMonetizationApi;

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

