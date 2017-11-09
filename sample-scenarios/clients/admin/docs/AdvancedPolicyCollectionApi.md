# AdvancedPolicyCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesAdvancedGet**](AdvancedPolicyCollectionApi.md#throttlingPoliciesAdvancedGet) | **GET** /throttling/policies/advanced | Get all Advanced throttling policies.
[**throttlingPoliciesAdvancedPost**](AdvancedPolicyCollectionApi.md#throttlingPoliciesAdvancedPost) | **POST** /throttling/policies/advanced | Add an Advanced Throttling Policy


<a name="throttlingPoliciesAdvancedGet"></a>
# **throttlingPoliciesAdvancedGet**
> AdvancedThrottlePolicyList throttlingPoliciesAdvancedGet(accept, ifNoneMatch, ifModifiedSince)

Get all Advanced throttling policies.

Retrieves all existing Advanced level throttling policies. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyCollectionApi;


AdvancedPolicyCollectionApi apiInstance = new AdvancedPolicyCollectionApi();
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    AdvancedThrottlePolicyList result = apiInstance.throttlingPoliciesAdvancedGet(accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AdvancedPolicyCollectionApi#throttlingPoliciesAdvancedGet");
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

[**AdvancedThrottlePolicyList**](AdvancedThrottlePolicyList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesAdvancedPost"></a>
# **throttlingPoliciesAdvancedPost**
> AdvancedThrottlePolicy throttlingPoliciesAdvancedPost(body, contentType)

Add an Advanced Throttling Policy

Add a new Advanced level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyCollectionApi;


AdvancedPolicyCollectionApi apiInstance = new AdvancedPolicyCollectionApi();
AdvancedThrottlePolicy body = new AdvancedThrottlePolicy(); // AdvancedThrottlePolicy | Advanced level policy object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    AdvancedThrottlePolicy result = apiInstance.throttlingPoliciesAdvancedPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AdvancedPolicyCollectionApi#throttlingPoliciesAdvancedPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**AdvancedThrottlePolicy**](AdvancedThrottlePolicy.md)| Advanced level policy object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**AdvancedThrottlePolicy**](AdvancedThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

