# CustomRulesCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesCustomGet**](CustomRulesCollectionApi.md#throttlingPoliciesCustomGet) | **GET** /throttling/policies/custom | Get all Custom Rules
[**throttlingPoliciesCustomPost**](CustomRulesCollectionApi.md#throttlingPoliciesCustomPost) | **POST** /throttling/policies/custom | Add a Custom Rule


<a name="throttlingPoliciesCustomGet"></a>
# **throttlingPoliciesCustomGet**
> CustomRuleList throttlingPoliciesCustomGet(accept, ifNoneMatch, ifModifiedSince)

Get all Custom Rules

Retrieves all Custom Rules.  **NOTE:** * Only super tenant users are allowed for this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.CustomRulesCollectionApi;


CustomRulesCollectionApi apiInstance = new CustomRulesCollectionApi();
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    CustomRuleList result = apiInstance.throttlingPoliciesCustomGet(accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CustomRulesCollectionApi#throttlingPoliciesCustomGet");
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

[**CustomRuleList**](CustomRuleList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesCustomPost"></a>
# **throttlingPoliciesCustomPost**
> CustomRule throttlingPoliciesCustomPost(body, contentType)

Add a Custom Rule

Adds a new Custom Rule.  **NOTE:** * Only super tenant users are allowed for this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.CustomRulesCollectionApi;


CustomRulesCollectionApi apiInstance = new CustomRulesCollectionApi();
CustomRule body = new CustomRule(); // CustomRule | Custom Rule object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    CustomRule result = apiInstance.throttlingPoliciesCustomPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CustomRulesCollectionApi#throttlingPoliciesCustomPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**CustomRule**](CustomRule.md)| Custom Rule object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**CustomRule**](CustomRule.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

