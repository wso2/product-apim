# AlertConfigurationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addAlertConfig**](AlertConfigurationApi.md#addAlertConfig) | **PUT** /alerts/{alertType}/configurations/{configurationId} | Add AbnormalRequestsPerMin Alert Configurations. 
[**deleteAlertConfig**](AlertConfigurationApi.md#deleteAlertConfig) | **DELETE** /alerts/{alertType}/configurations/{configurationId} | Delete the Selected Configuration from AbnormalRequestsPerMin Alert Type. 
[**getAllAlertConfigs**](AlertConfigurationApi.md#getAllAlertConfigs) | **GET** /alerts/{alertType}/configurations | Get All AbnormalRequestsPerMin Alert Configurations 


<a name="addAlertConfig"></a>
# **addAlertConfig**
> AlertConfigDTO addAlertConfig(alertType, configurationId, requestBody)

Add AbnormalRequestsPerMin Alert Configurations. 

This operation is used to add configuration for the AbnormalRequestsPerMin alert type. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AlertConfigurationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AlertConfigurationApi apiInstance = new AlertConfigurationApi(defaultClient);
    String alertType = "alertType_example"; // String | The alert type.
    String configurationId = "configurationId_example"; // String | The alert configuration id.
    Map<String, String> requestBody = new HashMap(); // Map<String, String> | Configuration for AbnormalRequestCount alert type
    try {
      AlertConfigDTO result = apiInstance.addAlertConfig(alertType, configurationId, requestBody);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AlertConfigurationApi#addAlertConfig");
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
 **alertType** | **String**| The alert type. |
 **configurationId** | **String**| The alert configuration id. |
 **requestBody** | [**Map&lt;String, String&gt;**](String.md)| Configuration for AbnormalRequestCount alert type |

### Return type

[**AlertConfigDTO**](AlertConfigDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with newly created object as entity. Location header contains URL of newly created entity.  |  * Location - The location of the newly created entity.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteAlertConfig"></a>
# **deleteAlertConfig**
> deleteAlertConfig(alertType, configurationId)

Delete the Selected Configuration from AbnormalRequestsPerMin Alert Type. 

This operation is used to delete configuration from the AbnormalRequestsPerMin alert type. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AlertConfigurationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AlertConfigurationApi apiInstance = new AlertConfigurationApi(defaultClient);
    String alertType = "alertType_example"; // String | The alert type.
    String configurationId = "configurationId_example"; // String | The alert configuration id.
    try {
      apiInstance.deleteAlertConfig(alertType, configurationId);
    } catch (ApiException e) {
      System.err.println("Exception when calling AlertConfigurationApi#deleteAlertConfig");
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
 **alertType** | **String**| The alert type. |
 **configurationId** | **String**| The alert configuration id. |

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
**200** | OK. The alert config is deleted successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAllAlertConfigs"></a>
# **getAllAlertConfigs**
> AlertConfigListDTO getAllAlertConfigs(alertType)

Get All AbnormalRequestsPerMin Alert Configurations 

This operation is used to get all configurations of the AbnormalRequestsPerMin alert type. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AlertConfigurationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AlertConfigurationApi apiInstance = new AlertConfigurationApi(defaultClient);
    String alertType = "alertType_example"; // String | The alert type.
    try {
      AlertConfigListDTO result = apiInstance.getAllAlertConfigs(alertType);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AlertConfigurationApi#getAllAlertConfigs");
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
 **alertType** | **String**| The alert type. |

### Return type

[**AlertConfigListDTO**](AlertConfigListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. The Developer Portal alert configuration.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |

