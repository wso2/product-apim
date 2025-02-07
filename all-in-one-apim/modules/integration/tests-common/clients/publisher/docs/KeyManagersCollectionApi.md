# KeyManagersCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllKeyManagers**](KeyManagersCollectionApi.md#getAllKeyManagers) | **GET** /key-managers | Get All Key Managers


<a name="getAllKeyManagers"></a>
# **getAllKeyManagers**
> KeyManagerListDTO getAllKeyManagers()

Get All Key Managers

Get all Key managers 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.KeyManagersCollectionApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    KeyManagersCollectionApi apiInstance = new KeyManagersCollectionApi(defaultClient);
    try {
      KeyManagerListDTO result = apiInstance.getAllKeyManagers();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling KeyManagersCollectionApi#getAllKeyManagers");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

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

