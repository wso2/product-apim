# SubscriptionIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsBlockSubscriptionPost**](SubscriptionIndividualApi.md#subscriptionsBlockSubscriptionPost) | **POST** /subscriptions/block-subscription | Block a subscription
[**subscriptionsSubscriptionIdGet**](SubscriptionIndividualApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get details of a subscription
[**subscriptionsSubscriptionIdUsageGet**](SubscriptionIndividualApi.md#subscriptionsSubscriptionIdUsageGet) | **GET** /subscriptions/{subscriptionId}/usage | Get details of a pending invoice for a monetized subscription with meterd billing.
[**subscriptionsUnblockSubscriptionPost**](SubscriptionIndividualApi.md#subscriptionsUnblockSubscriptionPost) | **POST** /subscriptions/unblock-subscription | Unblock a Subscription


<a name="subscriptionsBlockSubscriptionPost"></a>
# **subscriptionsBlockSubscriptionPost**
> subscriptionsBlockSubscriptionPost(subscriptionId, blockState, ifMatch)

Block a subscription

This operation can be used to block a subscription. Along with the request, &#x60;blockState&#x60; must be specified as a query parameter.  1. &#x60;BLOCKED&#x60; : Subscription is completely blocked for both Production and Sandbox environments. 2. &#x60;PROD_ONLY_BLOCKED&#x60; : Subscription is blocked for Production environment only. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String blockState = "blockState_example"; // String | Subscription block state. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.subscriptionsBlockSubscriptionPost(subscriptionId, blockState, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsBlockSubscriptionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |
 **blockState** | **String**| Subscription block state.  | [enum: BLOCKED, PROD_ONLY_BLOCKED]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsSubscriptionIdGet"></a>
# **subscriptionsSubscriptionIdGet**
> ExtendedSubscriptionDTO subscriptionsSubscriptionIdGet(subscriptionId, ifNoneMatch)

Get details of a subscription

This operation can be used to get details of a single subscription. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ExtendedSubscriptionDTO result = apiInstance.subscriptionsSubscriptionIdGet(subscriptionId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsSubscriptionIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ExtendedSubscriptionDTO**](ExtendedSubscriptionDTO.md)

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
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
try {
    APIMonetizationUsageDTO result = apiInstance.subscriptionsSubscriptionIdUsageGet(subscriptionId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsSubscriptionIdUsageGet");
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

<a name="subscriptionsUnblockSubscriptionPost"></a>
# **subscriptionsUnblockSubscriptionPost**
> subscriptionsUnblockSubscriptionPost(subscriptionId, ifMatch)

Unblock a Subscription

This operation can be used to unblock a subscription specifying the subscription Id. The subscription will be fully unblocked after performing this operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.subscriptionsUnblockSubscriptionPost(subscriptionId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsUnblockSubscriptionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

