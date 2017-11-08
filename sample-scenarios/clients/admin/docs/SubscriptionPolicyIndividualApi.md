# SubscriptionPolicyIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesSubscriptionPolicyIdDelete**](SubscriptionPolicyIndividualApi.md#throttlingPoliciesSubscriptionPolicyIdDelete) | **DELETE** /throttling/policies/subscription/{policyId} | Delete a Subscription Policy
[**throttlingPoliciesSubscriptionPolicyIdGet**](SubscriptionPolicyIndividualApi.md#throttlingPoliciesSubscriptionPolicyIdGet) | **GET** /throttling/policies/subscription/{policyId} | Get a Subscription Policy
[**throttlingPoliciesSubscriptionPolicyIdPut**](SubscriptionPolicyIndividualApi.md#throttlingPoliciesSubscriptionPolicyIdPut) | **PUT** /throttling/policies/subscription/{policyId} | Update a Subscription Policy


<a name="throttlingPoliciesSubscriptionPolicyIdDelete"></a>
# **throttlingPoliciesSubscriptionPolicyIdDelete**
> throttlingPoliciesSubscriptionPolicyIdDelete(policyId, ifMatch, ifUnmodifiedSince)

Delete a Subscription Policy

This operation can be used to delete a subscription-level throttling policy specifying the Id of the policy as a path paramter. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.SubscriptionPolicyIndividualApi;


SubscriptionPolicyIndividualApi apiInstance = new SubscriptionPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.throttlingPoliciesSubscriptionPolicyIdDelete(policyId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionPolicyIndividualApi#throttlingPoliciesSubscriptionPolicyIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| Thorttle policy UUID  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesSubscriptionPolicyIdGet"></a>
# **throttlingPoliciesSubscriptionPolicyIdGet**
> SubscriptionThrottlePolicy throttlingPoliciesSubscriptionPolicyIdGet(policyId, ifNoneMatch, ifModifiedSince)

Get a Subscription Policy

Retrieve a single subscription-level throttling policy. We should provide the Id of the policy as a path parameter. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.SubscriptionPolicyIndividualApi;


SubscriptionPolicyIndividualApi apiInstance = new SubscriptionPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    SubscriptionThrottlePolicy result = apiInstance.throttlingPoliciesSubscriptionPolicyIdGet(policyId, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionPolicyIndividualApi#throttlingPoliciesSubscriptionPolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| Thorttle policy UUID  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**SubscriptionThrottlePolicy**](SubscriptionThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesSubscriptionPolicyIdPut"></a>
# **throttlingPoliciesSubscriptionPolicyIdPut**
> SubscriptionThrottlePolicy throttlingPoliciesSubscriptionPolicyIdPut(policyId, body, contentType, ifMatch, ifUnmodifiedSince)

Update a Subscription Policy

Updates an existing subscription-level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.SubscriptionPolicyIndividualApi;


SubscriptionPolicyIndividualApi apiInstance = new SubscriptionPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
SubscriptionThrottlePolicy body = new SubscriptionThrottlePolicy(); // SubscriptionThrottlePolicy | Policy object that needs to be modified 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    SubscriptionThrottlePolicy result = apiInstance.throttlingPoliciesSubscriptionPolicyIdPut(policyId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionPolicyIndividualApi#throttlingPoliciesSubscriptionPolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| Thorttle policy UUID  |
 **body** | [**SubscriptionThrottlePolicy**](SubscriptionThrottlePolicy.md)| Policy object that needs to be modified  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**SubscriptionThrottlePolicy**](SubscriptionThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

