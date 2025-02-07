# ApiMonetizationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addAPIMonetization**](ApiMonetizationApi.md#addAPIMonetization) | **POST** /apis/{apiId}/monetize | Configure Monetization for a Given API
[**getAPIMonetization**](ApiMonetizationApi.md#getAPIMonetization) | **GET** /apis/{apiId}/monetization | Get Monetization Status for each Tier in a Given API
[**getAPIRevenue**](ApiMonetizationApi.md#getAPIRevenue) | **GET** /apis/{apiId}/revenue | Get Total Revenue Details of a Given Monetized API with Meterd Billing
[**getSubscriptionUsage**](ApiMonetizationApi.md#getSubscriptionUsage) | **GET** /subscriptions/{subscriptionId}/usage | Get Details of a Pending Invoice for a Monetized Subscription with Metered Billing.


<a name="addAPIMonetization"></a>
# **addAPIMonetization**
> addAPIMonetization(apiId, apIMonetizationInfoDTO)

Configure Monetization for a Given API

This operation can be used to configure monetization for a given API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMonetizationApi apiInstance = new ApiMonetizationApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIMonetizationInfoDTO apIMonetizationInfoDTO = new APIMonetizationInfoDTO(); // APIMonetizationInfoDTO | Monetization data object
    try {
      apiInstance.addAPIMonetization(apiId, apIMonetizationInfoDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMonetizationApi#addAPIMonetization");
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
 **apIMonetizationInfoDTO** | [**APIMonetizationInfoDTO**](APIMonetizationInfoDTO.md)| Monetization data object |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | OK. Monetization status changed successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIMonetization"></a>
# **getAPIMonetization**
> getAPIMonetization(apiId)

Get Monetization Status for each Tier in a Given API

This operation can be used to get monetization status for each tier in a given API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMonetizationApi apiInstance = new ApiMonetizationApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      apiInstance.getAPIMonetization(apiId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMonetizationApi#getAPIMonetization");
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

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Monetization status for each tier returned successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIRevenue"></a>
# **getAPIRevenue**
> APIRevenueDTO getAPIRevenue(apiId)

Get Total Revenue Details of a Given Monetized API with Meterd Billing

This operation can be used to get details of total revenue details of a given monetized API with meterd billing. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMonetizationApi apiInstance = new ApiMonetizationApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      APIRevenueDTO result = apiInstance.getAPIRevenue(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMonetizationApi#getAPIRevenue");
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

### Return type

[**APIRevenueDTO**](APIRevenueDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Details of a total revenue returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future). <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future). <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getSubscriptionUsage"></a>
# **getSubscriptionUsage**
> APIMonetizationUsageDTO getSubscriptionUsage(subscriptionId)

Get Details of a Pending Invoice for a Monetized Subscription with Metered Billing.

This operation can be used to get details of a pending invoice for a monetized subscription with meterd billing. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMonetizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMonetizationApi apiInstance = new ApiMonetizationApi(defaultClient);
    String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
    try {
      APIMonetizationUsageDTO result = apiInstance.getSubscriptionUsage(subscriptionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMonetizationApi#getSubscriptionUsage");
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
 **subscriptionId** | **String**| Subscription Id  |

### Return type

[**APIMonetizationUsageDTO**](APIMonetizationUsageDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Details of a pending invoice returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future). <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future). <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. Requested Subscription does not exist.  |  -  |

