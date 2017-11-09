# AdvancedPolicyIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesAdvancedPolicyIdDelete**](AdvancedPolicyIndividualApi.md#throttlingPoliciesAdvancedPolicyIdDelete) | **DELETE** /throttling/policies/advanced/{policyId} | Delete an Advanced Throttling Policy
[**throttlingPoliciesAdvancedPolicyIdGet**](AdvancedPolicyIndividualApi.md#throttlingPoliciesAdvancedPolicyIdGet) | **GET** /throttling/policies/advanced/{policyId} | Get an Advanced Policy
[**throttlingPoliciesAdvancedPolicyIdPut**](AdvancedPolicyIndividualApi.md#throttlingPoliciesAdvancedPolicyIdPut) | **PUT** /throttling/policies/advanced/{policyId} | Update an Advanced Throttling Policy


<a name="throttlingPoliciesAdvancedPolicyIdDelete"></a>
# **throttlingPoliciesAdvancedPolicyIdDelete**
> throttlingPoliciesAdvancedPolicyIdDelete(policyId, ifMatch, ifUnmodifiedSince)

Delete an Advanced Throttling Policy

Deletes an Advanced level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyIndividualApi;


AdvancedPolicyIndividualApi apiInstance = new AdvancedPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.throttlingPoliciesAdvancedPolicyIdDelete(policyId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling AdvancedPolicyIndividualApi#throttlingPoliciesAdvancedPolicyIdDelete");
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

<a name="throttlingPoliciesAdvancedPolicyIdGet"></a>
# **throttlingPoliciesAdvancedPolicyIdGet**
> AdvancedThrottlePolicy throttlingPoliciesAdvancedPolicyIdGet(policyId, ifNoneMatch, ifModifiedSince)

Get an Advanced Policy

Retrieves an Advanced Policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyIndividualApi;


AdvancedPolicyIndividualApi apiInstance = new AdvancedPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    AdvancedThrottlePolicy result = apiInstance.throttlingPoliciesAdvancedPolicyIdGet(policyId, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AdvancedPolicyIndividualApi#throttlingPoliciesAdvancedPolicyIdGet");
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

[**AdvancedThrottlePolicy**](AdvancedThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesAdvancedPolicyIdPut"></a>
# **throttlingPoliciesAdvancedPolicyIdPut**
> AdvancedThrottlePolicy throttlingPoliciesAdvancedPolicyIdPut(policyId, body, contentType, ifMatch, ifUnmodifiedSince)

Update an Advanced Throttling Policy

Updates an existing Advanced level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyIndividualApi;


AdvancedPolicyIndividualApi apiInstance = new AdvancedPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
AdvancedThrottlePolicy body = new AdvancedThrottlePolicy(); // AdvancedThrottlePolicy | Policy object that needs to be modified 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    AdvancedThrottlePolicy result = apiInstance.throttlingPoliciesAdvancedPolicyIdPut(policyId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AdvancedPolicyIndividualApi#throttlingPoliciesAdvancedPolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| Thorttle policy UUID  |
 **body** | [**AdvancedThrottlePolicy**](AdvancedThrottlePolicy.md)| Policy object that needs to be modified  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**AdvancedThrottlePolicy**](AdvancedThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

