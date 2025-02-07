# ApiMonetizationApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsSubscriptionIdUsageGet**](ApiMonetizationApi.md#subscriptionsSubscriptionIdUsageGet) | **GET** /subscriptions/{subscriptionId}/usage | Get Details of a Pending Invoice for a Monetized Subscription with Metered Billing.


<a name="subscriptionsSubscriptionIdUsageGet"></a>
# **subscriptionsSubscriptionIdUsageGet**
> APIMonetizationUsageDTO subscriptionsSubscriptionIdUsageGet(subscriptionId)

Get Details of a Pending Invoice for a Monetized Subscription with Metered Billing.

This operation can be used to get details of a pending invoice for a monetized subscription with metered billing. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiMonetizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMonetizationApi apiInstance = new ApiMonetizationApi(defaultClient);
    String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
    try {
      APIMonetizationUsageDTO result = apiInstance.subscriptionsSubscriptionIdUsageGet(subscriptionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMonetizationApi#subscriptionsSubscriptionIdUsageGet");
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
**404** | Not Found. The specified resource does not exist. |  -  |

