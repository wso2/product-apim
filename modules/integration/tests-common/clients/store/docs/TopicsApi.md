# TopicsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdTopicsGet**](TopicsApi.md#apisApiIdTopicsGet) | **GET** /apis/{apiId}/topics | Get a list of available topics for a given async API 


<a name="apisApiIdTopicsGet"></a>
# **apisApiIdTopicsGet**
> TopicListDTO apisApiIdTopicsGet(apiId, xWSO2Tenant)

Get a list of available topics for a given async API 

This operation will provide a list of topics available for a given Async API, based on the provided API ID. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.TopicsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    TopicsApi apiInstance = new TopicsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      TopicListDTO result = apiInstance.apisApiIdTopicsGet(apiId, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling TopicsApi#apisApiIdTopicsGet");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**TopicListDTO**](TopicListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Topic list returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

