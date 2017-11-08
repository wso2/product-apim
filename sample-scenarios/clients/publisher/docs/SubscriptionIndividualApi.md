# SubscriptionIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsBlockSubscriptionPost**](SubscriptionIndividualApi.md#subscriptionsBlockSubscriptionPost) | **POST** /subscriptions/block-subscription | Block a subscription
[**subscriptionsSubscriptionIdGet**](SubscriptionIndividualApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get details of a subscription
[**subscriptionsUnblockSubscriptionPost**](SubscriptionIndividualApi.md#subscriptionsUnblockSubscriptionPost) | **POST** /subscriptions/unblock-subscription | Unblock a Subscription


<a name="subscriptionsBlockSubscriptionPost"></a>
# **subscriptionsBlockSubscriptionPost**
> subscriptionsBlockSubscriptionPost(subscriptionId, blockState, ifMatch, ifUnmodifiedSince)

Block a subscription

This operation can be used to block a subscription. Along with the request, &#x60;blockState&#x60; must be specified as a query parameter.  1. &#x60;BLOCKED&#x60; : Subscription is completely blocked for both Production and Sandbox environments. 2. &#x60;PROD_ONLY_BLOCKED&#x60; : Subscription is blocked for Production environment only. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.SubscriptionIndividualApi;


SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String blockState = "blockState_example"; // String | Subscription block state. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.subscriptionsBlockSubscriptionPost(subscriptionId, blockState, ifMatch, ifUnmodifiedSince);
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsSubscriptionIdGet"></a>
# **subscriptionsSubscriptionIdGet**
> ExtendedSubscription subscriptionsSubscriptionIdGet(subscriptionId, accept, ifNoneMatch, ifModifiedSince)

Get details of a subscription

This operation can be used to get details of a single subscription. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.SubscriptionIndividualApi;


SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    ExtendedSubscription result = apiInstance.subscriptionsSubscriptionIdGet(subscriptionId, accept, ifNoneMatch, ifModifiedSince);
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
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**ExtendedSubscription**](ExtendedSubscription.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsUnblockSubscriptionPost"></a>
# **subscriptionsUnblockSubscriptionPost**
> subscriptionsUnblockSubscriptionPost(subscriptionId, ifMatch, ifUnmodifiedSince)

Unblock a Subscription

This operation can be used to unblock a subscription specifying the subscription Id. The subscription will be fully unblocked after performing this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.SubscriptionIndividualApi;


SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.subscriptionsUnblockSubscriptionPost(subscriptionId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsUnblockSubscriptionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

