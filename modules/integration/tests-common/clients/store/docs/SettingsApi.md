# SettingsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**settingsApplicationAttributesGet**](SettingsApi.md#settingsApplicationAttributesGet) | **GET** /settings/application-attributes | Get all application attributes from configuration 
[**settingsGet**](SettingsApi.md#settingsGet) | **GET** /settings | Retreive store settings


<a name="settingsApplicationAttributesGet"></a>
# **settingsApplicationAttributesGet**
> ApplicationAttributeListDTO settingsApplicationAttributesGet(ifNoneMatch)

Get all application attributes from configuration 

This operation can be used to retrieve the application attributes from configuration. It will not return hidden attributes. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SettingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SettingsApi apiInstance = new SettingsApi();
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    ApplicationAttributeListDTO result = apiInstance.settingsApplicationAttributesGet(ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SettingsApi#settingsApplicationAttributesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**ApplicationAttributeListDTO**](ApplicationAttributeListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="settingsGet"></a>
# **settingsGet**
> SettingsDTO settingsGet(xWSO2Tenant)

Retreive store settings

Retreive store settings 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SettingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SettingsApi apiInstance = new SettingsApi();
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    SettingsDTO result = apiInstance.settingsGet(xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SettingsApi#settingsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

[**SettingsDTO**](SettingsDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

