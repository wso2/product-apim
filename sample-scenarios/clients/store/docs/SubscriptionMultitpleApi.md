# SubscriptionMultitpleApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsMultiplePost**](SubscriptionMultitpleApi.md#subscriptionsMultiplePost) | **POST** /subscriptions/multiple | Add new subscriptions 


<a name="subscriptionsMultiplePost"></a>
# **subscriptionsMultiplePost**
> Subscription subscriptionsMultiplePost(body, contentType)

Add new subscriptions 

This operation can be used to add a new subscriptions providing the ids of the APIs and the applications. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.SubscriptionMultitpleApi;


SubscriptionMultitpleApi apiInstance = new SubscriptionMultitpleApi();
List<Subscription> body = Arrays.asList(new Subscription()); // List<Subscription> | Subscription objects that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    Subscription result = apiInstance.subscriptionsMultiplePost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionMultitpleApi#subscriptionsMultiplePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**List&lt;Subscription&gt;**](Subscription.md)| Subscription objects that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**Subscription**](Subscription.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

