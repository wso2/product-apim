# RecommendationsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**recommendationsGet**](RecommendationsApi.md#recommendationsGet) | **GET** /recommendations | Give API Recommendations for a User


<a name="recommendationsGet"></a>
# **recommendationsGet**
> RecommendationsDTO recommendationsGet()

Give API Recommendations for a User

This API can be used to get recommended APIs for a user who logs into the API Developer Portal

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.RecommendationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    RecommendationsApi apiInstance = new RecommendationsApi(defaultClient);
    try {
      RecommendationsDTO result = apiInstance.recommendationsGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling RecommendationsApi#recommendationsGet");
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

[**RecommendationsDTO**](RecommendationsDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested recommendations are returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |

