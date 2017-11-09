# SubscriptionCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsGet**](SubscriptionCollectionApi.md#subscriptionsGet) | **GET** /subscriptions | Get all subscriptions 


<a name="subscriptionsGet"></a>
# **subscriptionsGet**
> SubscriptionList subscriptionsGet(apiId, applicationId, groupId, offset, limit, accept, ifNoneMatch)

Get all subscriptions 

This operation can be used to retrieve a list of subscriptions of the user associated with the provided access token. This operation is capable of  1. Retrieving applications which are subscibed to a specific API. &#x60;GET https://127.0.0.1:9443/api/am/store/v0.11/subscriptions?apiId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60;  2. Retrieving APIs which are subscribed by a specific application. &#x60;GET https://127.0.0.1:9443/api/am/store/v0.11/subscriptions?applicationId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60;  **IMPORTANT:** * It is mandatory to provide either **apiId** or **applicationId**. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.SubscriptionCollectionApi;


SubscriptionCollectionApi apiInstance = new SubscriptionCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String groupId = "groupId_example"; // String | Application Group Id 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    SubscriptionList result = apiInstance.subscriptionsGet(apiId, applicationId, groupId, offset, limit, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionCollectionApi#subscriptionsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **groupId** | **String**| Application Group Id  | [optional]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**SubscriptionList**](SubscriptionList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

