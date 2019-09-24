# ThreatProtectionPolicyApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**threatProtectionPoliciesPolicyIdGet**](ThreatProtectionPolicyApi.md#threatProtectionPoliciesPolicyIdGet) | **GET** /threat-protection-policies/{policyId} | Get a threat protection policy


<a name="threatProtectionPoliciesPolicyIdGet"></a>
# **threatProtectionPoliciesPolicyIdGet**
> ThreatProtectionPolicyDTO threatProtectionPoliciesPolicyIdGet(policyId)

Get a threat protection policy

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ThreatProtectionPolicyApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ThreatProtectionPolicyApi apiInstance = new ThreatProtectionPolicyApi();
String policyId = "policyId_example"; // String | The UUID of a Policy 
try {
    ThreatProtectionPolicyDTO result = apiInstance.threatProtectionPoliciesPolicyIdGet(policyId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThreatProtectionPolicyApi#threatProtectionPoliciesPolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyId** | **String**| The UUID of a Policy  |

### Return type

[**ThreatProtectionPolicyDTO**](ThreatProtectionPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

