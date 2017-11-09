# ApplicationPolicyIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesApplicationPolicyIdDelete**](ApplicationPolicyIndividualApi.md#throttlingPoliciesApplicationPolicyIdDelete) | **DELETE** /throttling/policies/application/{policyId} | Delete an Application Throttling policy
[**throttlingPoliciesApplicationPolicyIdGet**](ApplicationPolicyIndividualApi.md#throttlingPoliciesApplicationPolicyIdGet) | **GET** /throttling/policies/application/{policyId} | Get an Application Policy
[**throttlingPoliciesApplicationPolicyIdPut**](ApplicationPolicyIndividualApi.md#throttlingPoliciesApplicationPolicyIdPut) | **PUT** /throttling/policies/application/{policyId} | Update an Application Throttling policy


<a name="throttlingPoliciesApplicationPolicyIdDelete"></a>
# **throttlingPoliciesApplicationPolicyIdDelete**
> throttlingPoliciesApplicationPolicyIdDelete(policyId, ifMatch, ifUnmodifiedSince)

Delete an Application Throttling policy

Deletes an Application level throttling policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.ApplicationPolicyIndividualApi;


ApplicationPolicyIndividualApi apiInstance = new ApplicationPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.throttlingPoliciesApplicationPolicyIdDelete(policyId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationPolicyIndividualApi#throttlingPoliciesApplicationPolicyIdDelete");
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

<a name="throttlingPoliciesApplicationPolicyIdGet"></a>
# **throttlingPoliciesApplicationPolicyIdGet**
> ApplicationThrottlePolicy throttlingPoliciesApplicationPolicyIdGet(policyId, ifNoneMatch, ifModifiedSince)

Get an Application Policy

Retrieves an Application Policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.ApplicationPolicyIndividualApi;


ApplicationPolicyIndividualApi apiInstance = new ApplicationPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    ApplicationThrottlePolicy result = apiInstance.throttlingPoliciesApplicationPolicyIdGet(policyId, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationPolicyIndividualApi#throttlingPoliciesApplicationPolicyIdGet");
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

[**ApplicationThrottlePolicy**](ApplicationThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesApplicationPolicyIdPut"></a>
# **throttlingPoliciesApplicationPolicyIdPut**
> ApplicationThrottlePolicy throttlingPoliciesApplicationPolicyIdPut(policyId, body, contentType, ifMatch, ifUnmodifiedSince)

Update an Application Throttling policy

Updates an existing Application level throttling policy. Upon succesfull, you will receive the updated application policy as the response. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.ApplicationPolicyIndividualApi;


ApplicationPolicyIndividualApi apiInstance = new ApplicationPolicyIndividualApi();
String policyId = "policyId_example"; // String | Thorttle policy UUID 
ApplicationThrottlePolicy body = new ApplicationThrottlePolicy(); // ApplicationThrottlePolicy | Policy object that needs to be modified 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    ApplicationThrottlePolicy result = apiInstance.throttlingPoliciesApplicationPolicyIdPut(policyId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationPolicyIndividualApi#throttlingPoliciesApplicationPolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| Thorttle policy UUID  |
 **body** | [**ApplicationThrottlePolicy**](ApplicationThrottlePolicy.md)| Policy object that needs to be modified  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**ApplicationThrottlePolicy**](ApplicationThrottlePolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

