# ThreatProtectionPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**threatProtectionPoliciesGet**](ThreatProtectionPoliciesApi.md#threatProtectionPoliciesGet) | **GET** /threat-protection-policies | Get All Threat Protection Policies


<a name="threatProtectionPoliciesGet"></a>
# **threatProtectionPoliciesGet**
> ThreatProtectionPolicyListDTO threatProtectionPoliciesGet()

Get All Threat Protection Policies

This can be used to get all defined threat protection policies

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ThreatProtectionPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ThreatProtectionPoliciesApi apiInstance = new ThreatProtectionPoliciesApi();
try {
    ThreatProtectionPolicyListDTO result = apiInstance.threatProtectionPoliciesGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThreatProtectionPoliciesApi#threatProtectionPoliciesGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ThreatProtectionPolicyListDTO**](ThreatProtectionPolicyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

