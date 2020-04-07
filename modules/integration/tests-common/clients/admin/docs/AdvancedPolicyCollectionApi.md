# AdvancedPolicyCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesAdvancedGet**](AdvancedPolicyCollectionApi.md#throttlingPoliciesAdvancedGet) | **GET** /throttling/policies/advanced | Get all Advanced throttling policies.
[**throttlingPoliciesAdvancedPost**](AdvancedPolicyCollectionApi.md#throttlingPoliciesAdvancedPost) | **POST** /throttling/policies/advanced | Add an Advanced Throttling Policy


<a name="throttlingPoliciesAdvancedGet"></a>
# **throttlingPoliciesAdvancedGet**
> AdvancedThrottlePolicyListDTO throttlingPoliciesAdvancedGet(accept, ifNoneMatch, ifModifiedSince)

Get all Advanced throttling policies.

Retrieves all existing Advanced level throttling policies. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.AdvancedPolicyCollectionApi;


AdvancedPolicyCollectionApi apiInstance = new AdvancedPolicyCollectionApi();
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    AdvancedThrottlePolicyListDTO result = apiInstance.throttlingPoliciesAdvancedGet(accept, ifNoneMatch, ifModifiedSince);
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

[**AdvancedThrottlePolicyListDTO**](AdvancedThrottlePolicyListDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesAdvancedPost"></a>
# **throttlingPoliciesAdvancedPost**
> AdvancedThrottlePolicyDTO throttlingPoliciesAdvancedPost(body, contentType)

Add an Advanced Throttling Policy

Add a new Advanced level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.AdvancedPolicyCollectionApi;


AdvancedPolicyCollectionApi apiInstance = new AdvancedPolicyCollectionApi();
AdvancedThrottlePolicyDTO body = new AdvancedThrottlePolicyDTO(); // AdvancedThrottlePolicyDTO | Advanced level policy object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    AdvancedThrottlePolicyDTO result = apiInstance.throttlingPoliciesAdvancedPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AdvancedPolicyCollectionApi#throttlingPoliciesAdvancedPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**AdvancedThrottlePolicyDTO**](AdvancedThrottlePolicyDTO.md)| Advanced level policy object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**AdvancedThrottlePolicyDTO**](AdvancedThrottlePolicyDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

