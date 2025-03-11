# LlmProviderApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getLLMProviderApiDefinition**](LlmProviderApi.md#getLLMProviderApiDefinition) | **GET** /llm-providers/{llmProviderId}/api-definition | Get LLM Provider&#39;s API Definition
[**getLLMProviderEndpointConfiguration**](LlmProviderApi.md#getLLMProviderEndpointConfiguration) | **GET** /llm-providers/{llmProviderId}/endpoint-configuration | Get LLM provider&#39;s security configurations
[**getLLMProviderModels**](LlmProviderApi.md#getLLMProviderModels) | **GET** /llm-providers/{llmProviderId}/models | Get LLM provider&#39;s model list


<a name="getLLMProviderApiDefinition"></a>
# **getLLMProviderApiDefinition**
> String getLLMProviderApiDefinition(llmProviderId)

Get LLM Provider&#39;s API Definition

Get LLM Provider&#39;s API Definition 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.LlmProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    LlmProviderApi apiInstance = new LlmProviderApi(defaultClient);
    String llmProviderId = "llmProviderId_example"; // String | 
    try {
      String result = apiInstance.getLLMProviderApiDefinition(llmProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling LlmProviderApi#getLLMProviderApiDefinition");
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
 **llmProviderId** | **String**|  |

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API Definition  |  -  |

<a name="getLLMProviderEndpointConfiguration"></a>
# **getLLMProviderEndpointConfiguration**
> LLMProviderEndpointConfigurationDTO getLLMProviderEndpointConfiguration(llmProviderId)

Get LLM provider&#39;s security configurations

Get LLM provider&#39;s endpoint security configurations 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.LlmProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    LlmProviderApi apiInstance = new LlmProviderApi(defaultClient);
    String llmProviderId = "llmProviderId_example"; // String | 
    try {
      LLMProviderEndpointConfigurationDTO result = apiInstance.getLLMProviderEndpointConfiguration(llmProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling LlmProviderApi#getLLMProviderEndpointConfiguration");
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
 **llmProviderId** | **String**|  |

### Return type

[**LLMProviderEndpointConfigurationDTO**](LLMProviderEndpointConfigurationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API Definition  |  -  |

<a name="getLLMProviderModels"></a>
# **getLLMProviderModels**
> List&lt;String&gt; getLLMProviderModels(llmProviderId)

Get LLM provider&#39;s model list

Get LLM provider&#39;s model list 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.LlmProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    LlmProviderApi apiInstance = new LlmProviderApi(defaultClient);
    String llmProviderId = "llmProviderId_example"; // String | 
    try {
      List<String> result = apiInstance.getLLMProviderModels(llmProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling LlmProviderApi#getLLMProviderModels");
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
 **llmProviderId** | **String**|  |

### Return type

**List&lt;String&gt;**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of models  |  -  |

