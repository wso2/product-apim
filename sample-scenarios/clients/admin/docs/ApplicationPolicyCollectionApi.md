# ApplicationPolicyCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesApplicationGet**](ApplicationPolicyCollectionApi.md#throttlingPoliciesApplicationGet) | **GET** /throttling/policies/application | Get all Application Throttling Policies
[**throttlingPoliciesApplicationPost**](ApplicationPolicyCollectionApi.md#throttlingPoliciesApplicationPost) | **POST** /throttling/policies/application | Add an Application Throttling Policy


<a name="throttlingPoliciesApplicationGet"></a>
# **throttlingPoliciesApplicationGet**
> ApplicationThrottlePolicyList throttlingPoliciesApplicationGet(accept, ifNoneMatch, ifModifiedSince)

Get all Application Throttling Policies

Retrieves all existing application throttling policies. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.ApplicationPolicyCollectionApi;


ApplicationPolicyCollectionApi apiInstance = new ApplicationPolicyCollectionApi();
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    ApplicationThrottlePolicyList result = apiInstance.throttlingPoliciesApplicationGet(accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationPolicyCollectionApi#throttlingPoliciesApplicationGet");
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

[**ApplicationThrottlePolicyList**](ApplicationThrottlePolicyList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesApplicationPost"></a>
# **throttlingPoliciesApplicationPost**
> ApplicationThrottlePolicy throttlingPoliciesApplicationPost(body, contentType)

Add an Application Throttling Policy

This operation can be used to add a new application level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.ApplicationPolicyCollectionApi;


ApplicationPolicyCollectionApi apiInstance = new ApplicationPolicyCollectionApi();
ApplicationThrottlePolicy body = new ApplicationThrottlePolicy(); // ApplicationThrottlePolicy | Application level policy object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    ApplicationThrottlePolicy result = apiInstance.throttlingPoliciesApplicationPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationPolicyCollectionApi#throttlingPoliciesApplicationPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ApplicationThrottlePolicy**](ApplicationThrottlePolicy.md)| Application level policy object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**ApplicationThrottlePolicy**](ApplicationThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

