# ApiResourcePoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdResourcePoliciesGet**](ApiResourcePoliciesApi.md#apisApiIdResourcePoliciesGet) | **GET** /apis/{apiId}/resource-policies | Get the resource policy (inflow/outflow) definitions
[**apisApiIdResourcePoliciesResourcePolicyIdGet**](ApiResourcePoliciesApi.md#apisApiIdResourcePoliciesResourcePolicyIdGet) | **GET** /apis/{apiId}/resource-policies/{resourcePolicyId} | Get the resource policy (inflow/outflow) definition for a given resource identifier.
[**apisApiIdResourcePoliciesResourcePolicyIdPut**](ApiResourcePoliciesApi.md#apisApiIdResourcePoliciesResourcePolicyIdPut) | **PUT** /apis/{apiId}/resource-policies/{resourcePolicyId} | Update the resource policy(inflow/outflow) definition for the given resource identifier


<a name="apisApiIdResourcePoliciesGet"></a>
# **apisApiIdResourcePoliciesGet**
> ResourcePolicyListDTO apisApiIdResourcePoliciesGet(apiId, sequenceType, resourcePath, verb, ifNoneMatch)

Get the resource policy (inflow/outflow) definitions

This operation can be used to retrieve conversion policy resource definitions of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiResourcePoliciesApi apiInstance = new ApiResourcePoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String sequenceType = "sequenceType_example"; // String | sequence type of the resource policy resource definition
String resourcePath = "resourcePath_example"; // String | Resource path of the resource policy definition
String verb = "verb_example"; // String | HTTP verb of the resource path of the resource policy definition
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ResourcePolicyListDTO result = apiInstance.apisApiIdResourcePoliciesGet(apiId, sequenceType, resourcePath, verb, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiResourcePoliciesApi#apisApiIdResourcePoliciesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **sequenceType** | **String**| sequence type of the resource policy resource definition |
 **resourcePath** | **String**| Resource path of the resource policy definition | [optional]
 **verb** | **String**| HTTP verb of the resource path of the resource policy definition | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ResourcePolicyListDTO**](ResourcePolicyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdResourcePoliciesResourcePolicyIdGet"></a>
# **apisApiIdResourcePoliciesResourcePolicyIdGet**
> ResourcePolicyInfoDTO apisApiIdResourcePoliciesResourcePolicyIdGet(apiId, resourcePolicyId, ifNoneMatch)

Get the resource policy (inflow/outflow) definition for a given resource identifier.

This operation can be used to retrieve conversion policy resource definitions of an API given the resource identifier. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiResourcePoliciesApi apiInstance = new ApiResourcePoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String resourcePolicyId = "resourcePolicyId_example"; // String | registry resource Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ResourcePolicyInfoDTO result = apiInstance.apisApiIdResourcePoliciesResourcePolicyIdGet(apiId, resourcePolicyId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiResourcePoliciesApi#apisApiIdResourcePoliciesResourcePolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **resourcePolicyId** | **String**| registry resource Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ResourcePolicyInfoDTO**](ResourcePolicyInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdResourcePoliciesResourcePolicyIdPut"></a>
# **apisApiIdResourcePoliciesResourcePolicyIdPut**
> ResourcePolicyInfoDTO apisApiIdResourcePoliciesResourcePolicyIdPut(apiId, resourcePolicyId, body, ifMatch)

Update the resource policy(inflow/outflow) definition for the given resource identifier

This operation can be used to update the resource policy(inflow/outflow) definition for the given resource identifier of an existing API. resource policy definition to be updated is passed as a body parameter &#x60;content&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiResourcePoliciesApi apiInstance = new ApiResourcePoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String resourcePolicyId = "resourcePolicyId_example"; // String | registry resource Id 
ResourcePolicyInfoDTO body = new ResourcePolicyInfoDTO(); // ResourcePolicyInfoDTO | Content of the resource policy definition that needs to be updated
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ResourcePolicyInfoDTO result = apiInstance.apisApiIdResourcePoliciesResourcePolicyIdPut(apiId, resourcePolicyId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiResourcePoliciesApi#apisApiIdResourcePoliciesResourcePolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **resourcePolicyId** | **String**| registry resource Id  |
 **body** | [**ResourcePolicyInfoDTO**](ResourcePolicyInfoDTO.md)| Content of the resource policy definition that needs to be updated |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ResourcePolicyInfoDTO**](ResourcePolicyInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

