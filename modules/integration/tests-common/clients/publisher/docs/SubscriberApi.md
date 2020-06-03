# SubscriberApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsSubscriptionIdSubscriberInfoGet**](SubscriberApi.md#subscriptionsSubscriptionIdSubscriberInfoGet) | **GET** /subscriptions/{subscriptionId}/subscriber-info | Get details of a user who subscribed an API


<a name="subscriptionsSubscriptionIdSubscriberInfoGet"></a>
# **subscriptionsSubscriptionIdSubscriberInfoGet**
> SubscriberInfoDTO subscriptionsSubscriptionIdSubscriberInfoGet(subscriptionId)

Get details of a user who subscribed an API

This operation can be used to get details of a user who subscribed to the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriberApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriberApi apiInstance = new SubscriberApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
try {
    SubscriberInfoDTO result = apiInstance.subscriptionsSubscriptionIdSubscriberInfoGet(subscriptionId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriberApi#subscriptionsSubscriptionIdSubscriberInfoGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |

### Return type

[**SubscriberInfoDTO**](SubscriberInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

