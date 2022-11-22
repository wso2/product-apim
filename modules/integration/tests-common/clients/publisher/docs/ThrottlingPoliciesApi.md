# ThrottlingPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllThrottlingPolicies**](ThrottlingPoliciesApi.md#getAllThrottlingPolicies) | **GET** /throttling-policies/{policyLevel} | Get All Throttling Policies for the Given Type
[**getSubscriptionThrottlingPolicies**](ThrottlingPoliciesApi.md#getSubscriptionThrottlingPolicies) | **GET** /throttling-policies/streaming/subscription | Get streaming throttling policies
[**getThrottlingPolicyByName**](ThrottlingPoliciesApi.md#getThrottlingPolicyByName) | **GET** /throttling-policies/{policyLevel}/{policyName} | Get Details of a Policy


<a name="getAllThrottlingPolicies"></a>
# **getAllThrottlingPolicies**
> ThrottlingPolicyListDTO getAllThrottlingPolicies(policyLevel, limit, offset, ifNoneMatch)

Get All Throttling Policies for the Given Type

This operation can be used to list the available policies for a given policy level. Tier level should be specified as a path parameter and should be one of &#x60;subscription&#x60; and &#x60;api&#x60;. &#x60;subscription&#x60; is for Subscription Level policies and &#x60;api&#x60; is for Resource Level policies 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi(defaultClient);
    String policyLevel = "policyLevel_example"; // String | List API or Application or Resource type policies. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      ThrottlingPolicyListDTO result = apiInstance.getAllThrottlingPolicies(policyLevel, limit, offset, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ThrottlingPoliciesApi#getAllThrottlingPolicies");
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
 **policyLevel** | **String**| List API or Application or Resource type policies.  | [enum: api, subcription]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ThrottlingPolicyListDTO**](ThrottlingPolicyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of policies returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getSubscriptionThrottlingPolicies"></a>
# **getSubscriptionThrottlingPolicies**
> SubscriptionPolicyListDTO getSubscriptionThrottlingPolicies(limit, offset, ifNoneMatch)

Get streaming throttling policies

This operation can be used to list the available streaming subscription policies 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      SubscriptionPolicyListDTO result = apiInstance.getSubscriptionThrottlingPolicies(limit, offset, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ThrottlingPoliciesApi#getSubscriptionThrottlingPolicies");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**SubscriptionPolicyListDTO**](SubscriptionPolicyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of subscription policies returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getThrottlingPolicyByName"></a>
# **getThrottlingPolicyByName**
> ThrottlingPolicyDTO getThrottlingPolicyByName(policyName, policyLevel, ifNoneMatch)

Get Details of a Policy

This operation can be used to retrieve details of a single policy by specifying the policy level and policy name. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ThrottlingPoliciesApi apiInstance = new ThrottlingPoliciesApi(defaultClient);
    String policyName = "policyName_example"; // String | Tier name 
    String policyLevel = "policyLevel_example"; // String | List API or Application or Resource type policies. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      ThrottlingPolicyDTO result = apiInstance.getThrottlingPolicyByName(policyName, policyLevel, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ThrottlingPoliciesApi#getThrottlingPolicyByName");
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
 **policyName** | **String**| Tier name  |
 **policyLevel** | **String**| List API or Application or Resource type policies.  | [enum: api, subcription]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ThrottlingPolicyDTO**](ThrottlingPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Tier returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

