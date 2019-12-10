# ThrottlingPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllThrottlingPolicies**](ThrottlingPoliciesApi.md#getAllThrottlingPolicies) | **GET** /throttling-policies/{policyLevel} | Get all throttling policies for the given type
[**getThrottlingPolicyByName**](ThrottlingPoliciesApi.md#getThrottlingPolicyByName) | **GET** /throttling-policies/{policyLevel}/{policyName} | Get details of a policy


<a name="getAllThrottlingPolicies"></a>
# **getAllThrottlingPolicies**
> ThrottlingPolicyListDTO getAllThrottlingPolicies(policyLevel, limit, offset, ifNoneMatch)

Get all throttling policies for the given type

This operation can be used to list the available policies for a given policy level. Tier level should be specified as a path parameter and should be one of &#x60;subscription&#x60; and &#x60;api&#x60;. &#x60;subscription&#x60; is for Subscription Level policies and &#x60;api&#x60; is for Resource Level policies 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi();
String policyLevel = "policyLevel_example"; // String | List API or Application or Resource type policies. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ThrottlingPolicyListDTO result = apiInstance.getAllThrottlingPolicies(policyLevel, limit, offset, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingPoliciesApi#getAllThrottlingPolicies");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyLevel** | **String**| List API or Application or Resource type policies.  | [enum: api, subcription]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ThrottlingPolicyListDTO**](ThrottlingPolicyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getThrottlingPolicyByName"></a>
# **getThrottlingPolicyByName**
> ThrottlingPolicyDTO getThrottlingPolicyByName(policyName, policyLevel, ifNoneMatch)

Get details of a policy

This operation can be used to retrieve details of a single policy by specifying the policy level and policy name. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi();
String policyName = "policyName_example"; // String | Tier name 
String policyLevel = "policyLevel_example"; // String | List API or Application or Resource type policies. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ThrottlingPolicyDTO result = apiInstance.getThrottlingPolicyByName(policyName, policyLevel, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingPoliciesApi#getThrottlingPolicyByName");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyName** | **String**| Tier name  |
 **policyLevel** | **String**| List API or Application or Resource type policies.  | [enum: api, subcription]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ThrottlingPolicyDTO**](ThrottlingPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

