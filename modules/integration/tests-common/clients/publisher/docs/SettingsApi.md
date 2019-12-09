# SettingsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**settingsGatewayEnvironmentsGet**](SettingsApi.md#settingsGatewayEnvironmentsGet) | **GET** /settings/gateway-environments | Get all gateway environments
[**settingsGet**](SettingsApi.md#settingsGet) | **GET** /settings | Retreive publisher settings


<a name="settingsGatewayEnvironmentsGet"></a>
# **settingsGatewayEnvironmentsGet**
> EnvironmentListDTO settingsGatewayEnvironmentsGet(apiId)

Get all gateway environments

This operation can be used to retrieve the list of gateway environments available. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SettingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SettingsApi apiInstance = new SettingsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
try {
    EnvironmentListDTO result = apiInstance.settingsGatewayEnvironmentsGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SettingsApi#settingsGatewayEnvironmentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |

### Return type

[**EnvironmentListDTO**](EnvironmentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="settingsGet"></a>
# **settingsGet**
> SettingsDTO settingsGet()

Retreive publisher settings

Retreive publisher settings 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.SettingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SettingsApi apiInstance = new SettingsApi();
try {
    SettingsDTO result = apiInstance.settingsGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SettingsApi#settingsGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SettingsDTO**](SettingsDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

