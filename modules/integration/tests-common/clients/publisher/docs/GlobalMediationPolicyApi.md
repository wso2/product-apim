# GlobalMediationPolicyApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getGlobalMediationPolicyContent**](GlobalMediationPolicyApi.md#getGlobalMediationPolicyContent) | **GET** /mediation-policies/{mediationPolicyId}/content | Downloadt specific global mediation policy


<a name="getGlobalMediationPolicyContent"></a>
# **getGlobalMediationPolicyContent**
> getGlobalMediationPolicyContent(mediationPolicyId, ifNoneMatch)

Downloadt specific global mediation policy

This operation can be used to download a particular global mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.GlobalMediationPolicyApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

GlobalMediationPolicyApi apiInstance = new GlobalMediationPolicyApi();
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.getGlobalMediationPolicyContent(mediationPolicyId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling GlobalMediationPolicyApi#getGlobalMediationPolicyContent");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

