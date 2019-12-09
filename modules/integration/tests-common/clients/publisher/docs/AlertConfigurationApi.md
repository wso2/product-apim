# AlertConfigurationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addAlertConfig**](AlertConfigurationApi.md#addAlertConfig) | **PUT** /alerts/{alertType}/configurations/{configurationId} | Add AbnormalRequestsPerMin alert configurations. 
[**deleteAlertConfig**](AlertConfigurationApi.md#deleteAlertConfig) | **DELETE** /alerts/{alertType}/configurations/{configurationId} | Delete the selected configuration from AbnormalRequestsPerMin alert type. 
[**getAllAlertConfigs**](AlertConfigurationApi.md#getAllAlertConfigs) | **GET** /alerts/{alertType}/configurations | Get all AbnormalRequestsPerMin alert configurations 


<a name="addAlertConfig"></a>
# **addAlertConfig**
> AlertConfigDTO addAlertConfig(alertType, configurationId, body)

Add AbnormalRequestsPerMin alert configurations. 

This operation is used to add configuration for the AbnormalRequestsPerMin alert type. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertConfigurationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertConfigurationApi apiInstance = new AlertConfigurationApi();
String alertType = "alertType_example"; // String | The alert type.
String configurationId = "configurationId_example"; // String | The alert configuration id.
AlertConfigInfoDTO body = new AlertConfigInfoDTO(); // AlertConfigInfoDTO | Configuration for AbnormalRequestCount alert type
try {
    AlertConfigDTO result = apiInstance.addAlertConfig(alertType, configurationId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertConfigurationApi#addAlertConfig");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alertType** | **String**| The alert type. |
 **configurationId** | **String**| The alert configuration id. |
 **body** | [**AlertConfigInfoDTO**](AlertConfigInfoDTO.md)| Configuration for AbnormalRequestCount alert type |

### Return type

[**AlertConfigDTO**](AlertConfigDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteAlertConfig"></a>
# **deleteAlertConfig**
> deleteAlertConfig(alertType, configurationId)

Delete the selected configuration from AbnormalRequestsPerMin alert type. 

This operation is used to delete configuration from the AbnormalRequestsPerMin alert type. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertConfigurationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertConfigurationApi apiInstance = new AlertConfigurationApi();
String alertType = "alertType_example"; // String | The alert type.
String configurationId = "configurationId_example"; // String | The alert configuration id.
try {
    apiInstance.deleteAlertConfig(alertType, configurationId);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertConfigurationApi#deleteAlertConfig");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getAllAlertConfigs"></a>
# **getAllAlertConfigs**
> AlertConfigListDTO getAllAlertConfigs(alertType)

Get all AbnormalRequestsPerMin alert configurations 

This operation is used to get all configurations of the AbnormalRequestsPerMin alert type. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertConfigurationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertConfigurationApi apiInstance = new AlertConfigurationApi();
String alertType = "alertType_example"; // String | The alert type.
try {
    AlertConfigListDTO result = apiInstance.getAllAlertConfigs(alertType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertConfigurationApi#getAllAlertConfigs");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

