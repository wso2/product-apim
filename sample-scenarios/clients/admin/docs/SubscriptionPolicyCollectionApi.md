# SubscriptionPolicyCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesSubscriptionGet**](SubscriptionPolicyCollectionApi.md#throttlingPoliciesSubscriptionGet) | **GET** /throttling/policies/subscription | Get all Subscription Throttling Policies
[**throttlingPoliciesSubscriptionPost**](SubscriptionPolicyCollectionApi.md#throttlingPoliciesSubscriptionPost) | **POST** /throttling/policies/subscription | Add a Subscription Throttling Policy


<a name="throttlingPoliciesSubscriptionGet"></a>
# **throttlingPoliciesSubscriptionGet**
> SubscriptionThrottlePolicyList throttlingPoliciesSubscriptionGet(accept, ifNoneMatch, ifModifiedSince)

Get all Subscription Throttling Policies

This operation can be used to retrieve all Subscription level throttling policies. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.SubscriptionPolicyCollectionApi;


SubscriptionPolicyCollectionApi apiInstance = new SubscriptionPolicyCollectionApi();
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    SubscriptionThrottlePolicyList result = apiInstance.throttlingPoliciesSubscriptionGet(accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionPolicyCollectionApi#throttlingPoliciesSubscriptionGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**SubscriptionThrottlePolicyList**](SubscriptionThrottlePolicyList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesSubscriptionPost"></a>
# **throttlingPoliciesSubscriptionPost**
> SubscriptionThrottlePolicy throttlingPoliciesSubscriptionPost(body, contentType)

Add a Subscription Throttling Policy

This operation can be used to add a Subscription level throttling policy specifying the details of the policy in the payload. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.SubscriptionPolicyCollectionApi;


SubscriptionPolicyCollectionApi apiInstance = new SubscriptionPolicyCollectionApi();
SubscriptionThrottlePolicy body = new SubscriptionThrottlePolicy(); // SubscriptionThrottlePolicy | Subscripion level policy object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    SubscriptionThrottlePolicy result = apiInstance.throttlingPoliciesSubscriptionPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionPolicyCollectionApi#throttlingPoliciesSubscriptionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**SubscriptionThrottlePolicy**](SubscriptionThrottlePolicy.md)| Subscripion level policy object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**SubscriptionThrottlePolicy**](SubscriptionThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

