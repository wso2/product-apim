# SdKsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdSdksLanguageGet**](SdKsApi.md#apisApiIdSdksLanguageGet) | **GET** /apis/{apiId}/sdks/{language} | Generate a SDK for an API 
[**sdkGenLanguagesGet**](SdKsApi.md#sdkGenLanguagesGet) | **GET** /sdk-gen/languages | Get a list of supported SDK languages 


<a name="apisApiIdSdksLanguageGet"></a>
# **apisApiIdSdksLanguageGet**
> byte[] apisApiIdSdksLanguageGet(apiId, language, xWSO2Tenant)

Generate a SDK for an API 

This operation can be used to generate SDKs (System Development Kits), for the APIs available in the API Store, for a requested development language. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SdKsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SdKsApi apiInstance = new SdKsApi();
String apiId = "apiId_example"; // String | ID of the specific API for which the SDK is required. 
String language = "language_example"; // String | Programming language of the SDK that is required. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    byte[] result = apiInstance.apisApiIdSdksLanguageGet(apiId, language, xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SdKsApi#apisApiIdSdksLanguageGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| ID of the specific API for which the SDK is required.  |
 **language** | **String**| Programming language of the SDK that is required.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

**byte[]**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/zip

<a name="sdkGenLanguagesGet"></a>
# **sdkGenLanguagesGet**
> sdkGenLanguagesGet()

Get a list of supported SDK languages 

This operation will provide a list of programming languages that are supported by the swagger codegen library for generating System Development Kits (SDKs) for APIs available in the API Manager Store 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SdKsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SdKsApi apiInstance = new SdKsApi();
try {
    apiInstance.sdkGenLanguagesGet();
} catch (ApiException e) {
    System.err.println("Exception when calling SdKsApi#sdkGenLanguagesGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

