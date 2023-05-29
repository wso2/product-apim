# SubscriberApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getSubscriberInfoBySubscriptionId**](SubscriberApi.md#getSubscriberInfoBySubscriptionId) | **GET** /subscriptions/{subscriptionId}/subscriber-info | Get Details of a Subscriber


<a name="getSubscriberInfoBySubscriptionId"></a>
# **getSubscriberInfoBySubscriptionId**
> SubscriberInfoDTO getSubscriberInfoBySubscriptionId(subscriptionId)

Get Details of a Subscriber

This operation can be used to get details of a user who subscribed to the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.SubscriberApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriberApi apiInstance = new SubscriberApi(defaultClient);
    String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
    try {
      SubscriberInfoDTO result = apiInstance.getSubscriberInfoBySubscriptionId(subscriptionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriberApi#getSubscriberInfoBySubscriptionId");
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

[**SubscriberInfoDTO**](SubscriberInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK.  Details of the subscriber are returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

