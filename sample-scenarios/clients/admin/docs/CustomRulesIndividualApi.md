# CustomRulesIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesCustomRuleIdDelete**](CustomRulesIndividualApi.md#throttlingPoliciesCustomRuleIdDelete) | **DELETE** /throttling/policies/custom/{ruleId} | Delete a Custom Rule
[**throttlingPoliciesCustomRuleIdGet**](CustomRulesIndividualApi.md#throttlingPoliciesCustomRuleIdGet) | **GET** /throttling/policies/custom/{ruleId} | Get a Custom Rule
[**throttlingPoliciesCustomRuleIdPut**](CustomRulesIndividualApi.md#throttlingPoliciesCustomRuleIdPut) | **PUT** /throttling/policies/custom/{ruleId} | Update a Custom Rule


<a name="throttlingPoliciesCustomRuleIdDelete"></a>
# **throttlingPoliciesCustomRuleIdDelete**
> throttlingPoliciesCustomRuleIdDelete(ruleId, ifMatch, ifUnmodifiedSince)

Delete a Custom Rule

Delete a Custom Rule. We need to provide the Id of the policy as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.CustomRulesIndividualApi;


CustomRulesIndividualApi apiInstance = new CustomRulesIndividualApi();
String ruleId = "ruleId_example"; // String | Custom rule UUID 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.throttlingPoliciesCustomRuleIdDelete(ruleId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling CustomRulesIndividualApi#throttlingPoliciesCustomRuleIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ruleId** | **String**| Custom rule UUID  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesCustomRuleIdGet"></a>
# **throttlingPoliciesCustomRuleIdGet**
> CustomRule throttlingPoliciesCustomRuleIdGet(ruleId, ifNoneMatch, ifModifiedSince)

Get a Custom Rule

Retrieves a Custom Rule. We need to provide the policy Id as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.CustomRulesIndividualApi;


CustomRulesIndividualApi apiInstance = new CustomRulesIndividualApi();
String ruleId = "ruleId_example"; // String | Custom rule UUID 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    CustomRule result = apiInstance.throttlingPoliciesCustomRuleIdGet(ruleId, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CustomRulesIndividualApi#throttlingPoliciesCustomRuleIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ruleId** | **String**| Custom rule UUID  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**CustomRule**](CustomRule.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesCustomRuleIdPut"></a>
# **throttlingPoliciesCustomRuleIdPut**
> CustomRule throttlingPoliciesCustomRuleIdPut(ruleId, body, contentType, ifMatch, ifUnmodifiedSince)

Update a Custom Rule

Updates an existing Custom Rule.  **NOTE:** * Only super tenant users are allowed for this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.CustomRulesIndividualApi;


CustomRulesIndividualApi apiInstance = new CustomRulesIndividualApi();
String ruleId = "ruleId_example"; // String | Custom rule UUID 
CustomRule body = new CustomRule(); // CustomRule | Policy object that needs to be modified 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    CustomRule result = apiInstance.throttlingPoliciesCustomRuleIdPut(ruleId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CustomRulesIndividualApi#throttlingPoliciesCustomRuleIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ruleId** | **String**| Custom rule UUID  |
 **body** | [**CustomRule**](CustomRule.md)| Policy object that needs to be modified  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**CustomRule**](CustomRule.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

