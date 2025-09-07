# LlmProvidersApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getLLMProviders**](LlmProvidersApi.md#getLLMProviders) | **GET** /llm-providers | Get all LLM providers


<a name="getLLMProviders"></a>
# **getLLMProviders**
> LLMProviderSummaryResponseListDTO getLLMProviders()

Get all LLM providers

Get all LLM providers 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.LlmProvidersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    LlmProvidersApi apiInstance = new LlmProvidersApi(defaultClient);
    try {
      LLMProviderSummaryResponseListDTO result = apiInstance.getLLMProviders();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling LlmProvidersApi#getLLMProviders");
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

[**LLMProviderSummaryResponseListDTO**](LLMProviderSummaryResponseListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of LLM providers.  |  * Content-Type - The content type of the body. <br>  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

