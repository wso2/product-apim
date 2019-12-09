# SubscriptionsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsBlockSubscriptionPost**](SubscriptionsApi.md#subscriptionsBlockSubscriptionPost) | **POST** /subscriptions/block-subscription | Block a subscription
[**subscriptionsGet**](SubscriptionsApi.md#subscriptionsGet) | **GET** /subscriptions | Get all Subscriptions
[**subscriptionsUnblockSubscriptionPost**](SubscriptionsApi.md#subscriptionsUnblockSubscriptionPost) | **POST** /subscriptions/unblock-subscription | Unblock a Subscription


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
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String blockState = "blockState_example"; // String | Subscription block state. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.subscriptionsBlockSubscriptionPost(subscriptionId, blockState, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsBlockSubscriptionPost");
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

<a name="subscriptionsGet"></a>
# **subscriptionsGet**
> SubscriptionListDTO subscriptionsGet(apiId, limit, offset, ifNoneMatch, query)

Get all Subscriptions

This operation can be used to retrieve a list of subscriptions of the user associated with the provided access token. This operation is capable of  1. Retrieving all subscriptions for the user&#39;s APIs. &#x60;GET https://127.0.0.1:9443/api/am/publisher/v1/subscriptions&#x60;  2. Retrieving subscriptions for a specific API. &#x60;GET https://127.0.0.1:9443/api/am/publisher/v1/subscriptions?apiId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60; 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String query = "query_example"; // String | Keywords to filter subscriptions 
try {
    SubscriptionListDTO result = apiInstance.subscriptionsGet(apiId, limit, offset, ifNoneMatch, query);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **query** | **String**| Keywords to filter subscriptions  | [optional]

### Return type

[**SubscriptionListDTO**](SubscriptionListDTO.md)

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
//import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.subscriptionsUnblockSubscriptionPost(subscriptionId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsUnblockSubscriptionPost");
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

