# KeyManagersCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**keyManagersGet**](KeyManagersCollectionApi.md#keyManagersGet) | **GET** /key-managers | Get All Key Managers


<a name="keyManagersGet"></a>
# **keyManagersGet**
> KeyManagerListDTO keyManagersGet(xWSO2Tenant)

Get All Key Managers

Get all Key managers 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.KeyManagersCollectionApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    KeyManagersCollectionApi apiInstance = new KeyManagersCollectionApi(defaultClient);
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      KeyManagerListDTO result = apiInstance.keyManagersGet(xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling KeyManagersCollectionApi#keyManagersGet");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**KeyManagerListDTO**](KeyManagerListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Categories returned  |  -  |

