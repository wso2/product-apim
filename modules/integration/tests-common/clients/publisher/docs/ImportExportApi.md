# ImportExportApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisExportGet**](ImportExportApi.md#apisExportGet) | **GET** /apis/export | Export an API


<a name="apisExportGet"></a>
# **apisExportGet**
> File apisExportGet(apiId, name, version, providerName, format, preserveStatus)

Export an API

This operation can be used to export the details of a particular API as a zip file. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ImportExportApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ImportExportApi apiInstance = new ImportExportApi();
String apiId = "apiId_example"; // String | UUID of the API
String name = "name_example"; // String | API Name 
String version = "version_example"; // String | Version of the API 
String providerName = "providerName_example"; // String | Provider name of the API 
String format = "format_example"; // String | Format of output documents. Can be YAML or JSON. 
Boolean preserveStatus = true; // Boolean | Preserve API Status on export 
try {
    File result = apiInstance.apisExportGet(apiId, name, version, providerName, format, preserveStatus);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImportExportApi#apisExportGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| UUID of the API | [optional]
 **name** | **String**| API Name  | [optional]
 **version** | **String**| Version of the API  | [optional]
 **providerName** | **String**| Provider name of the API  | [optional]
 **format** | **String**| Format of output documents. Can be YAML or JSON.  | [optional] [enum: JSON, YAML]
 **preserveStatus** | **Boolean**| Preserve API Status on export  | [optional]

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/zip

