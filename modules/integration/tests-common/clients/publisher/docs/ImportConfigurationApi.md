# ImportConfigurationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**importApisPost**](ImportConfigurationApi.md#importApisPost) | **POST** /import/apis | Imports API(s).
[**importApisPut**](ImportConfigurationApi.md#importApisPut) | **PUT** /import/apis | Imports API(s).


<a name="importApisPost"></a>
# **importApisPost**
> APIListDTO importApisPost(file, provider)

Imports API(s).

This operation can be used to import one or more existing APIs. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ImportConfigurationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ImportConfigurationApi apiInstance = new ImportConfigurationApi();
File file = new File("/path/to/file.txt"); // File | Zip archive consisting on exported api configuration 
String provider = "provider_example"; // String | If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to. 
try {
    APIListDTO result = apiInstance.importApisPost(file, provider);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImportConfigurationApi#importApisPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| Zip archive consisting on exported api configuration  |
 **provider** | **String**| If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to.  | [optional]

### Return type

[**APIListDTO**](APIListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="importApisPut"></a>
# **importApisPut**
> APIListDTO importApisPut(file, provider)

Imports API(s).

This operation can be used to import one or more existing APIs. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ImportConfigurationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ImportConfigurationApi apiInstance = new ImportConfigurationApi();
File file = new File("/path/to/file.txt"); // File | Zip archive consisting on exported api configuration 
String provider = "provider_example"; // String | If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to. 
try {
    APIListDTO result = apiInstance.importApisPut(file, provider);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImportConfigurationApi#importApisPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| Zip archive consisting on exported api configuration  |
 **provider** | **String**| If defined, updates the existing provider of each API with the specified provider. This is to cater scenarios where the current API provider does not exist in the environment that the API is imported to.  | [optional]

### Return type

[**APIListDTO**](APIListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

