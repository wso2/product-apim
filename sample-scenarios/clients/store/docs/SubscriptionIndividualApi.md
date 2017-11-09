# SubscriptionIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsPost**](SubscriptionIndividualApi.md#subscriptionsPost) | **POST** /subscriptions | Add a new subscription 
[**subscriptionsSubscriptionIdDelete**](SubscriptionIndividualApi.md#subscriptionsSubscriptionIdDelete) | **DELETE** /subscriptions/{subscriptionId} | Remove a subscription 
[**subscriptionsSubscriptionIdGet**](SubscriptionIndividualApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get details of a subscription 


<a name="subscriptionsPost"></a>
# **subscriptionsPost**
> Subscription subscriptionsPost(body, contentType)

Add a new subscription 

This operation can be used to add a new subscription providing the id of the API and the application. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.SubscriptionIndividualApi;


SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
Subscription body = new Subscription(); // Subscription | Subscription object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    Subscription result = apiInstance.subscriptionsPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Subscription**](Subscription.md)| Subscription object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**Subscription**](Subscription.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsSubscriptionIdDelete"></a>
# **subscriptionsSubscriptionIdDelete**
> subscriptionsSubscriptionIdDelete(subscriptionId, ifMatch, ifUnmodifiedSince)

Remove a subscription 

This operation can be used to remove a subscription. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.SubscriptionIndividualApi;


SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.subscriptionsSubscriptionIdDelete(subscriptionId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionIndividualApi#subscriptionsSubscriptionIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **subscriptionId** | **String**| Subscription Id  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
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
> Subscription subscriptionsSubscriptionIdGet(subscriptionId, accept, ifNoneMatch, ifModifiedSince)

Get details of a subscription 

This operation can be used to get details of a single subscription. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.SubscriptionIndividualApi;


SubscriptionIndividualApi apiInstance = new SubscriptionIndividualApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
try {
    Subscription result = apiInstance.subscriptionsSubscriptionIdGet(subscriptionId, accept, ifNoneMatch, ifModifiedSince);
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]

### Return type

[**Subscription**](Subscription.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

