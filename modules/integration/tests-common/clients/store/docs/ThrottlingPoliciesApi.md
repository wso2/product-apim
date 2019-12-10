# ThrottlingPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingPoliciesPolicyLevelGet**](ThrottlingPoliciesApi.md#throttlingPoliciesPolicyLevelGet) | **GET** /throttling-policies/{policyLevel} | Get all available throttling policies
[**throttlingPoliciesPolicyLevelPolicyIdGet**](ThrottlingPoliciesApi.md#throttlingPoliciesPolicyLevelPolicyIdGet) | **GET** /throttling-policies/{policyLevel}/{policyId} | Get details of a throttling policy 


<a name="throttlingPoliciesPolicyLevelGet"></a>
# **throttlingPoliciesPolicyLevelGet**
> List&lt;ThrottlingPolicyListDTO&gt; throttlingPoliciesPolicyLevelGet(policyLevel, limit, offset, ifNoneMatch, xWSO2Tenant)

Get all available throttling policies

Get available Throttling Policies 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ThrottlingPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi();
String policyLevel = "policyLevel_example"; // String | List Application or Subscription type thro. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    List<ThrottlingPolicyListDTO> result = apiInstance.throttlingPoliciesPolicyLevelGet(policyLevel, limit, offset, ifNoneMatch, xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingPoliciesApi#throttlingPoliciesPolicyLevelGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyLevel** | **String**| List Application or Subscription type thro.  | [enum: application, subscription]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

[**List&lt;ThrottlingPolicyListDTO&gt;**](ThrottlingPolicyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingPoliciesPolicyLevelPolicyIdGet"></a>
# **throttlingPoliciesPolicyLevelPolicyIdGet**
> ThrottlingPolicyDTO throttlingPoliciesPolicyLevelPolicyIdGet(policyId, policyLevel, xWSO2Tenant, ifNoneMatch)

Get details of a throttling policy 

This operation can be used to retrieve details of a single throttling policy by specifying the policy level and policy name.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive throttling policy that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ThrottlingPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi();
String policyId = "policyId_example"; // String | Policy Id represented as a UUID 
String policyLevel = "policyLevel_example"; // String | List Application or Subscription type thro. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    ThrottlingPolicyDTO result = apiInstance.throttlingPoliciesPolicyLevelPolicyIdGet(policyId, policyLevel, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingPoliciesApi#throttlingPoliciesPolicyLevelPolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| Policy Id represented as a UUID  |
 **policyLevel** | **String**| List Application or Subscription type thro.  | [enum: application, subscription]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**ThrottlingPolicyDTO**](ThrottlingPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

