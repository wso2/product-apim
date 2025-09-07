# AiServiceProviderApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAIServiceProvider**](AiServiceProviderApi.md#getAIServiceProvider) | **GET** /ai-service-providers/{aiServiceProviderId} | Get AI Service Provider
[**getAIServiceProviderApiDefinition**](AiServiceProviderApi.md#getAIServiceProviderApiDefinition) | **GET** /ai-service-providers/{aiServiceProviderId}/api-definition | Get AI Service Provider&#39;s API Definition
[**getAIServiceProviderEndpointConfiguration**](AiServiceProviderApi.md#getAIServiceProviderEndpointConfiguration) | **GET** /ai-service-providers/{aiServiceProviderId}/endpoint-configuration | Get AI Service Provider&#39;s security configurations
[**getAIServiceProviderModels**](AiServiceProviderApi.md#getAIServiceProviderModels) | **GET** /ai-service-providers/{aiServiceProviderId}/models | Get AI Service Provider&#39;s model list


<a name="getAIServiceProvider"></a>
# **getAIServiceProvider**
> AIServiceProviderResponseDTO getAIServiceProvider(aiServiceProviderId)

Get AI Service Provider

Get a AI Service Provider 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AiServiceProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AiServiceProviderApi apiInstance = new AiServiceProviderApi(defaultClient);
    String aiServiceProviderId = "aiServiceProviderId_example"; // String | AI Service Provider ID 
    try {
      AIServiceProviderResponseDTO result = apiInstance.getAIServiceProvider(aiServiceProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AiServiceProviderApi#getAIServiceProvider");
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
 **aiServiceProviderId** | **String**| AI Service Provider ID  |

### Return type

[**AIServiceProviderResponseDTO**](AIServiceProviderResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. AI Service Provider  |  -  |

<a name="getAIServiceProviderApiDefinition"></a>
# **getAIServiceProviderApiDefinition**
> String getAIServiceProviderApiDefinition(aiServiceProviderId)

Get AI Service Provider&#39;s API Definition

Get AI Service Provider&#39;s API Definition 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AiServiceProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AiServiceProviderApi apiInstance = new AiServiceProviderApi(defaultClient);
    String aiServiceProviderId = "aiServiceProviderId_example"; // String | 
    try {
      String result = apiInstance.getAIServiceProviderApiDefinition(aiServiceProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AiServiceProviderApi#getAIServiceProviderApiDefinition");
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
 **aiServiceProviderId** | **String**|  |

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

<a name="getAIServiceProviderEndpointConfiguration"></a>
# **getAIServiceProviderEndpointConfiguration**
> AIServiceProviderEndpointConfigurationDTO getAIServiceProviderEndpointConfiguration(aiServiceProviderId)

Get AI Service Provider&#39;s security configurations

Get AI Service Provider&#39;s endpoint security configurations 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AiServiceProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AiServiceProviderApi apiInstance = new AiServiceProviderApi(defaultClient);
    String aiServiceProviderId = "aiServiceProviderId_example"; // String | 
    try {
      AIServiceProviderEndpointConfigurationDTO result = apiInstance.getAIServiceProviderEndpointConfiguration(aiServiceProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AiServiceProviderApi#getAIServiceProviderEndpointConfiguration");
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
 **aiServiceProviderId** | **String**|  |

### Return type

[**AIServiceProviderEndpointConfigurationDTO**](AIServiceProviderEndpointConfigurationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API Definition  |  -  |

<a name="getAIServiceProviderModels"></a>
# **getAIServiceProviderModels**
> List&lt;ModelProviderDTO&gt; getAIServiceProviderModels(aiServiceProviderId)

Get AI Service Provider&#39;s model list

Get AI Service Provider&#39;s model list 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AiServiceProviderApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AiServiceProviderApi apiInstance = new AiServiceProviderApi(defaultClient);
    String aiServiceProviderId = "aiServiceProviderId_example"; // String | 
    try {
      List<ModelProviderDTO> result = apiInstance.getAIServiceProviderModels(aiServiceProviderId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AiServiceProviderApi#getAIServiceProviderModels");
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
 **aiServiceProviderId** | **String**|  |

### Return type

[**List&lt;ModelProviderDTO&gt;**](ModelProviderDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of supported model families grouped by vendor  |  -  |

