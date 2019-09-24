# ApiIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDelete**](ApiIndividualApi.md#apisApiIdDelete) | **DELETE** /apis/{apiId} | Delete an API
[**apisApiIdGet**](ApiIndividualApi.md#apisApiIdGet) | **GET** /apis/{apiId} | Get details of an API
[**apisApiIdLifecycleHistoryGet**](ApiIndividualApi.md#apisApiIdLifecycleHistoryGet) | **GET** /apis/{apiId}/lifecycle-history | Get Lifecycle state change history of the API.
[**apisApiIdLifecycleStateGet**](ApiIndividualApi.md#apisApiIdLifecycleStateGet) | **GET** /apis/{apiId}/lifecycle-state | Get Lifecycle state data of the API.
[**apisApiIdLifecycleStatePendingTasksDelete**](ApiIndividualApi.md#apisApiIdLifecycleStatePendingTasksDelete) | **DELETE** /apis/{apiId}/lifecycle-state/pending-tasks | Delete pending lifecycle state change tasks.
[**apisApiIdMonetizationGet**](ApiIndividualApi.md#apisApiIdMonetizationGet) | **GET** /apis/{apiId}/monetization | Get monetization status for each tier in a given API
[**apisApiIdMonetizePost**](ApiIndividualApi.md#apisApiIdMonetizePost) | **POST** /apis/{apiId}/monetize | Configure monetization for a given API
[**apisApiIdPut**](ApiIndividualApi.md#apisApiIdPut) | **PUT** /apis/{apiId} | Update an API
[**apisApiIdResourcePoliciesGet**](ApiIndividualApi.md#apisApiIdResourcePoliciesGet) | **GET** /apis/{apiId}/resource-policies | Get the resource policy (inflow/outflow) definitions
[**apisApiIdResourcePoliciesResourcePolicyIdGet**](ApiIndividualApi.md#apisApiIdResourcePoliciesResourcePolicyIdGet) | **GET** /apis/{apiId}/resource-policies/{resourcePolicyId} | Get the resource policy (inflow/outflow) definition for a given resource identifier.
[**apisApiIdResourcePoliciesResourcePolicyIdPut**](ApiIndividualApi.md#apisApiIdResourcePoliciesResourcePolicyIdPut) | **PUT** /apis/{apiId}/resource-policies/{resourcePolicyId} | Update the resource policy(inflow/outflow) definition for the given resource identifier
[**apisApiIdRevenueGet**](ApiIndividualApi.md#apisApiIdRevenueGet) | **GET** /apis/{apiId}/revenue | Get total revenue details of a given monetized API with meterd billing.
[**apisApiIdSubscriptionPoliciesGet**](ApiIndividualApi.md#apisApiIdSubscriptionPoliciesGet) | **GET** /apis/{apiId}/subscription-policies | Get details of the subscription throttling policies of an API 
[**apisApiIdSwaggerGet**](ApiIndividualApi.md#apisApiIdSwaggerGet) | **GET** /apis/{apiId}/swagger | Get swagger definition
[**apisApiIdSwaggerPut**](ApiIndividualApi.md#apisApiIdSwaggerPut) | **PUT** /apis/{apiId}/swagger | Update swagger definition
[**apisApiIdThreatProtectionPoliciesDelete**](ApiIndividualApi.md#apisApiIdThreatProtectionPoliciesDelete) | **DELETE** /apis/{apiId}/threat-protection-policies | Delete a threat protection policy from an API
[**apisApiIdThreatProtectionPoliciesGet**](ApiIndividualApi.md#apisApiIdThreatProtectionPoliciesGet) | **GET** /apis/{apiId}/threat-protection-policies | Get all threat protection policies associated with an API
[**apisApiIdThreatProtectionPoliciesPost**](ApiIndividualApi.md#apisApiIdThreatProtectionPoliciesPost) | **POST** /apis/{apiId}/threat-protection-policies | Add a threat protection policy to an API
[**apisApiIdThumbnailGet**](ApiIndividualApi.md#apisApiIdThumbnailGet) | **GET** /apis/{apiId}/thumbnail | Get thumbnail image
[**apisApiIdWsdlGet**](ApiIndividualApi.md#apisApiIdWsdlGet) | **GET** /apis/{apiId}/wsdl | Get WSDL definition
[**apisApiIdWsdlPut**](ApiIndividualApi.md#apisApiIdWsdlPut) | **PUT** /apis/{apiId}/wsdl | Update WSDL definition
[**apisChangeLifecyclePost**](ApiIndividualApi.md#apisChangeLifecyclePost) | **POST** /apis/change-lifecycle | Change API Status
[**apisCopyApiPost**](ApiIndividualApi.md#apisCopyApiPost) | **POST** /apis/copy-api | Create a new API version
[**apisPost**](ApiIndividualApi.md#apisPost) | **POST** /apis | Create a new API
[**updateAPIThumbnail**](ApiIndividualApi.md#updateAPIThumbnail) | **PUT** /apis/{apiId}/thumbnail | Upload a thumbnail image


<a name="apisApiIdDelete"></a>
# **apisApiIdDelete**
> apisApiIdDelete(apiId, ifMatch)

Delete an API

This operation can be used to delete an existing API proving the Id of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdDelete(apiId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdGet"></a>
# **apisApiIdGet**
> APIDTO apisApiIdGet(apiId, xWSO2Tenant, ifNoneMatch)

Get details of an API

Using this operation, you can retrieve complete details of a single API. You need to provide the Id of the API to retrive it. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    APIDTO result = apiInstance.apisApiIdGet(apiId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdLifecycleHistoryGet"></a>
# **apisApiIdLifecycleHistoryGet**
> LifecycleHistoryDTO apisApiIdLifecycleHistoryGet(apiId, ifNoneMatch)

Get Lifecycle state change history of the API.

This operation can be used to retrieve Lifecycle state change history of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    LifecycleHistoryDTO result = apiInstance.apisApiIdLifecycleHistoryGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdLifecycleHistoryGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**LifecycleHistoryDTO**](LifecycleHistoryDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdLifecycleStateGet"></a>
# **apisApiIdLifecycleStateGet**
> LifecycleStateDTO apisApiIdLifecycleStateGet(apiId, ifNoneMatch)

Get Lifecycle state data of the API.

This operation can be used to retrieve Lifecycle state data of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    LifecycleStateDTO result = apiInstance.apisApiIdLifecycleStateGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdLifecycleStateGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**LifecycleStateDTO**](LifecycleStateDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdLifecycleStatePendingTasksDelete"></a>
# **apisApiIdLifecycleStatePendingTasksDelete**
> apisApiIdLifecycleStatePendingTasksDelete(apiId)

Delete pending lifecycle state change tasks.

This operation can be used to remove pending lifecycle state change requests that are in pending state 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    apiInstance.apisApiIdLifecycleStatePendingTasksDelete(apiId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdLifecycleStatePendingTasksDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMonetizationGet"></a>
# **apisApiIdMonetizationGet**
> apisApiIdMonetizationGet(apiId)

Get monetization status for each tier in a given API

This operation can be used to get monetization status for each tier in a given API 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    apiInstance.apisApiIdMonetizationGet(apiId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdMonetizationGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMonetizePost"></a>
# **apisApiIdMonetizePost**
> apisApiIdMonetizePost(apiId, body)

Configure monetization for a given API

This operation can be used to configure monetization for a given API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
APIMonetizationInfoDTO body = new APIMonetizationInfoDTO(); // APIMonetizationInfoDTO | Monetization data object 
try {
    apiInstance.apisApiIdMonetizePost(apiId, body);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdMonetizePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**APIMonetizationInfoDTO**](APIMonetizationInfoDTO.md)| Monetization data object  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdPut"></a>
# **apisApiIdPut**
> APIDTO apisApiIdPut(apiId, body, ifMatch)

Update an API

This operation can be used to update an existing API. But the properties &#x60;name&#x60;, &#x60;version&#x60;, &#x60;context&#x60;, &#x60;provider&#x60;, &#x60;state&#x60; will not be changed by this operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
APIDTO body = new APIDTO(); // APIDTO | API object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIDTO result = apiInstance.apisApiIdPut(apiId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**APIDTO**](APIDTO.md)| API object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

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
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String sequenceType = "sequenceType_example"; // String | sequence type of the resource policy resource definition
String resourcePath = "resourcePath_example"; // String | Resource path of the resource policy definition
String verb = "verb_example"; // String | HTTP verb of the resource path of the resource policy definition
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ResourcePolicyListDTO result = apiInstance.apisApiIdResourcePoliciesGet(apiId, sequenceType, resourcePath, verb, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdResourcePoliciesGet");
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
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String resourcePolicyId = "resourcePolicyId_example"; // String | registry resource Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ResourcePolicyInfoDTO result = apiInstance.apisApiIdResourcePoliciesResourcePolicyIdGet(apiId, resourcePolicyId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdResourcePoliciesResourcePolicyIdGet");
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
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String resourcePolicyId = "resourcePolicyId_example"; // String | registry resource Id 
ResourcePolicyInfoDTO body = new ResourcePolicyInfoDTO(); // ResourcePolicyInfoDTO | Content of the resource policy definition that needs to be updated
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ResourcePolicyInfoDTO result = apiInstance.apisApiIdResourcePoliciesResourcePolicyIdPut(apiId, resourcePolicyId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdResourcePoliciesResourcePolicyIdPut");
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

<a name="apisApiIdRevenueGet"></a>
# **apisApiIdRevenueGet**
> APIRevenueDTO apisApiIdRevenueGet(apiId)

Get total revenue details of a given monetized API with meterd billing.

This operation can be used to get details of total revenue details of a given monetized API with meterd billing. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    APIRevenueDTO result = apiInstance.apisApiIdRevenueGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdRevenueGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

[**APIRevenueDTO**](APIRevenueDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSubscriptionPoliciesGet"></a>
# **apisApiIdSubscriptionPoliciesGet**
> ThrottlingPolicyDTO apisApiIdSubscriptionPoliciesGet(apiId, xWSO2Tenant, ifNoneMatch)

Get details of the subscription throttling policies of an API 

This operation can be used to retrieve details of the subscription throttling policy of an API by specifying the API Id.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive API subscription throttling policies that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ThrottlingPolicyDTO result = apiInstance.apisApiIdSubscriptionPoliciesGet(apiId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdSubscriptionPoliciesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ThrottlingPolicyDTO**](ThrottlingPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSwaggerGet"></a>
# **apisApiIdSwaggerGet**
> String apisApiIdSwaggerGet(apiId, ifNoneMatch)

Get swagger definition

This operation can be used to retrieve the swagger definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    String result = apiInstance.apisApiIdSwaggerGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdSwaggerGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSwaggerPut"></a>
# **apisApiIdSwaggerPut**
> apisApiIdSwaggerPut(apiId, apiDefinition, ifMatch)

Update swagger definition

This operation can be used to update the swagger definition of an existing API. Swagger definition to be updated is passed as a form data parameter &#x60;apiDefinition&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String apiDefinition = "apiDefinition_example"; // String | Swagger definition of the API
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdSwaggerPut(apiId, apiDefinition, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdSwaggerPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **apiDefinition** | **String**| Swagger definition of the API |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisApiIdThreatProtectionPoliciesDelete"></a>
# **apisApiIdThreatProtectionPoliciesDelete**
> apisApiIdThreatProtectionPoliciesDelete(apiId, policyId)

Delete a threat protection policy from an API

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String policyId = "policyId_example"; // String | Threat protection policy id
try {
    apiInstance.apisApiIdThreatProtectionPoliciesDelete(apiId, policyId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdThreatProtectionPoliciesDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **policyId** | **String**| Threat protection policy id |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdThreatProtectionPoliciesGet"></a>
# **apisApiIdThreatProtectionPoliciesGet**
> List&lt;String&gt; apisApiIdThreatProtectionPoliciesGet(apiId)

Get all threat protection policies associated with an API

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    List<String> result = apiInstance.apisApiIdThreatProtectionPoliciesGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdThreatProtectionPoliciesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

**List&lt;String&gt;**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdThreatProtectionPoliciesPost"></a>
# **apisApiIdThreatProtectionPoliciesPost**
> apisApiIdThreatProtectionPoliciesPost(apiId, policyId)

Add a threat protection policy to an API

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String policyId = "policyId_example"; // String | Threat protection policy id
try {
    apiInstance.apisApiIdThreatProtectionPoliciesPost(apiId, policyId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdThreatProtectionPoliciesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **policyId** | **String**| Threat protection policy id |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdThumbnailGet"></a>
# **apisApiIdThumbnailGet**
> apisApiIdThumbnailGet(apiId, ifNoneMatch)

Get thumbnail image

This operation can be used to download a thumbnail image of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apisApiIdThumbnailGet(apiId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdThumbnailGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdWsdlGet"></a>
# **apisApiIdWsdlGet**
> apisApiIdWsdlGet(apiId, ifNoneMatch)

Get WSDL definition

This operation can be used to retrieve the WSDL definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apisApiIdWsdlGet(apiId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdWsdlGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/octet-stream

<a name="apisApiIdWsdlPut"></a>
# **apisApiIdWsdlPut**
> apisApiIdWsdlPut(apiId, file, ifMatch)

Update WSDL definition

This operation can be used to update the WSDL definition of an existing API. WSDL to be updated is passed as a form data parameter &#x60;inlineContent&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
File file = new File("/path/to/file.txt"); // File | WSDL file or archive to upload
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdWsdlPut(apiId, file, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisApiIdWsdlPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **file** | **File**| WSDL file or archive to upload |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisChangeLifecyclePost"></a>
# **apisChangeLifecyclePost**
> WorkflowResponseDTO apisChangeLifecyclePost(action, apiId, lifecycleChecklist, ifMatch)

Change API Status

This operation is used to change the lifecycle of an API. Eg: Publish an API which is in &#x60;CREATED&#x60; state. In order to change the lifecycle, we need to provide the lifecycle &#x60;action&#x60; as a query parameter.  For example, to Publish an API, &#x60;action&#x60; should be &#x60;Publish&#x60;. Note that the &#x60;Re-publish&#x60; action is available only after calling &#x60;Block&#x60;.  Some actions supports providing additional paramters which should be provided as &#x60;lifecycleChecklist&#x60; parameter. Please see parameters table for more information. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String action = "action_example"; // String | The action to demote or promote the state of the API.  Supported actions are [ **Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Block, Deprecate, Re-Publish, Retire **] 
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
String lifecycleChecklist = "lifecycleChecklist_example"; // String |  Supported checklist items are as follows. 1. **Deprecate Old Versions**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Require Re-Subscription**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version.  You can specify additional checklist items by using an **\"attribute:\"** modifier.  Eg: \"Deprecate Old Versions:true\" will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \"attribute1:true, attribute2:false\" format.  **Sample CURL :**  curl -k -H \"Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\" -X POST \"https://localhost:9443/api/am/publisher/v1/apis/change-lifecycle?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&action=Publish&lifecycleChecklist=Deprecate Old Versions:true,Require Re-Subscription:true\" 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    WorkflowResponseDTO result = apiInstance.apisChangeLifecyclePost(action, apiId, lifecycleChecklist, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisChangeLifecyclePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **action** | **String**| The action to demote or promote the state of the API.  Supported actions are [ **Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Block, Deprecate, Re-Publish, Retire **]  | [enum: Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Block, Deprecate, Re-Publish, Retire]
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **lifecycleChecklist** | **String**|  Supported checklist items are as follows. 1. **Deprecate Old Versions**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Require Re-Subscription**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version.  You can specify additional checklist items by using an **\&quot;attribute:\&quot;** modifier.  Eg: \&quot;Deprecate Old Versions:true\&quot; will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \&quot;attribute1:true, attribute2:false\&quot; format.  **Sample CURL :**  curl -k -H \&quot;Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v1/apis/change-lifecycle?apiId&#x3D;890a4f4d-09eb-4877-a323-57f6ce2ed79b&amp;action&#x3D;Publish&amp;lifecycleChecklist&#x3D;Deprecate Old Versions:true,Require Re-Subscription:true\&quot;  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**WorkflowResponseDTO**](WorkflowResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisCopyApiPost"></a>
# **apisCopyApiPost**
> apisCopyApiPost(newVersion, apiId, defaultVersion)

Create a new API version

This operation can be used to create a new version of an existing API. The new version is specified as &#x60;newVersion&#x60; query parameter. New API will be in &#x60;CREATED&#x60; state. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String newVersion = "newVersion_example"; // String | Version of the new API.
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
Boolean defaultVersion = false; // Boolean | Specifies whether new API should be added as default version.
try {
    apiInstance.apisCopyApiPost(newVersion, apiId, defaultVersion);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisCopyApiPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **newVersion** | **String**| Version of the new API. |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **defaultVersion** | **Boolean**| Specifies whether new API should be added as default version. | [optional] [default to false]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisPost"></a>
# **apisPost**
> APIDTO apisPost(body)

Create a new API

This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
APIDTO body = new APIDTO(); // APIDTO | API object that needs to be added 
try {
    APIDTO result = apiInstance.apisPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#apisPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**APIDTO**](APIDTO.md)| API object that needs to be added  |

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="updateAPIThumbnail"></a>
# **updateAPIThumbnail**
> FileInfoDTO updateAPIThumbnail(apiId, file, ifMatch)

Upload a thumbnail image

This operation can be used to upload a thumbnail image of an API. The thumbnail to be uploaded should be given as a form data parameter &#x60;file&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiIndividualApi apiInstance = new ApiIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
File file = new File("/path/to/file.txt"); // File | Image to upload
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    FileInfoDTO result = apiInstance.updateAPIThumbnail(apiId, file, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#updateAPIThumbnail");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **file** | **File**| Image to upload |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**FileInfoDTO**](FileInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

