# ApiResourcePoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAPIResourcePolicies**](ApiResourcePoliciesApi.md#getAPIResourcePolicies) | **GET** /apis/{apiId}/resource-policies | Get the Resource Policy(inflow/outflow) Definitions
[**getAPIResourcePoliciesByPolicyId**](ApiResourcePoliciesApi.md#getAPIResourcePoliciesByPolicyId) | **GET** /apis/{apiId}/resource-policies/{resourcePolicyId} | Get the Resource Policy(inflow/outflow) Definition for a Given Resource Identifier.
[**updateAPIResourcePoliciesByPolicyId**](ApiResourcePoliciesApi.md#updateAPIResourcePoliciesByPolicyId) | **PUT** /apis/{apiId}/resource-policies/{resourcePolicyId} | Update the Resource Policy(inflow/outflow) Definition for the Given Resource Identifier


<a name="getAPIResourcePolicies"></a>
# **getAPIResourcePolicies**
> ResourcePolicyListDTO getAPIResourcePolicies(apiId, sequenceType, resourcePath, verb, ifNoneMatch)

Get the Resource Policy(inflow/outflow) Definitions

This operation can be used to retrieve conversion policy resource definitions of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiResourcePoliciesApi apiInstance = new ApiResourcePoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String sequenceType = "sequenceType_example"; // String | sequence type of the resource policy resource definition
    String resourcePath = "resourcePath_example"; // String | Resource path of the resource policy definition
    String verb = "verb_example"; // String | HTTP verb of the resource path of the resource policy definition
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      ResourcePolicyListDTO result = apiInstance.getAPIResourcePolicies(apiId, sequenceType, resourcePath, verb, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiResourcePoliciesApi#getAPIResourcePolicies");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
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

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of resource policy definitions of the API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIResourcePoliciesByPolicyId"></a>
# **getAPIResourcePoliciesByPolicyId**
> ResourcePolicyInfoDTO getAPIResourcePoliciesByPolicyId(apiId, resourcePolicyId, ifNoneMatch)

Get the Resource Policy(inflow/outflow) Definition for a Given Resource Identifier.

This operation can be used to retrieve conversion policy resource definitions of an API given the resource identifier. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiResourcePoliciesApi apiInstance = new ApiResourcePoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String resourcePolicyId = "resourcePolicyId_example"; // String | registry resource Id 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      ResourcePolicyInfoDTO result = apiInstance.getAPIResourcePoliciesByPolicyId(apiId, resourcePolicyId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiResourcePoliciesApi#getAPIResourcePoliciesByPolicyId");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
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

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested resource policy definition of the API is returned for the given resource identifier.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="updateAPIResourcePoliciesByPolicyId"></a>
# **updateAPIResourcePoliciesByPolicyId**
> ResourcePolicyInfoDTO updateAPIResourcePoliciesByPolicyId(apiId, resourcePolicyId, resourcePolicyInfoDTO, ifMatch)

Update the Resource Policy(inflow/outflow) Definition for the Given Resource Identifier

This operation can be used to update the resource policy(inflow/outflow) definition for the given resource identifier of an existing API. resource policy definition to be updated is passed as a body parameter &#x60;content&#x60;. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiResourcePoliciesApi apiInstance = new ApiResourcePoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String resourcePolicyId = "resourcePolicyId_example"; // String | registry resource Id 
    ResourcePolicyInfoDTO resourcePolicyInfoDTO = new ResourcePolicyInfoDTO(); // ResourcePolicyInfoDTO | Content of the resource policy definition that needs to be updated
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      ResourcePolicyInfoDTO result = apiInstance.updateAPIResourcePoliciesByPolicyId(apiId, resourcePolicyId, resourcePolicyInfoDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiResourcePoliciesApi#updateAPIResourcePoliciesByPolicyId");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **resourcePolicyId** | **String**| registry resource Id  |
 **resourcePolicyInfoDTO** | [**ResourcePolicyInfoDTO**](ResourcePolicyInfoDTO.md)| Content of the resource policy definition that needs to be updated |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ResourcePolicyInfoDTO**](ResourcePolicyInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated the resource policy definition  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

