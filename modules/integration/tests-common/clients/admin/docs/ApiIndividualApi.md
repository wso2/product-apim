# ApiIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**exportApiGet**](ApiIndividualApi.md#exportApiGet) | **GET** /export/api | Export an API
[**importApiPost**](ApiIndividualApi.md#importApiPost) | **POST** /import/api | Import an API


<a name="exportApiGet"></a>
# **exportApiGet**
> File exportApiGet(name, version, providerName, format, preserveStatus)

Export an API

This operation can be used to export the details of a particular API as a zip file. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApiIndividualApi;


ApiIndividualApi apiInstance = new ApiIndividualApi();
String name = "name_example"; // String | API Name 
String version = "version_example"; // String | Version of the API 
String providerName = "providerName_example"; // String | Provider name of the API 
String format = "format_example"; // String | Format of output documents. Can be YAML or JSON. 
Boolean preserveStatus = true; // Boolean | Preserve API Status on export 
try {
    File result = apiInstance.exportApiGet(name, version, providerName, format, preserveStatus);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#exportApiGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **String**| API Name  |
 **version** | **String**| Version of the API  |
 **providerName** | **String**| Provider name of the API  |
 **format** | **String**| Format of output documents. Can be YAML or JSON.  | [enum: JSON, YAML]
 **preserveStatus** | **Boolean**| Preserve API Status on export  | [optional]

### Return type

[**File**](File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/zip

<a name="importApiPost"></a>
# **importApiPost**
> importApiPost(file, preserveProvider, overwrite)

Import an API

This operation can be used to import an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApiIndividualApi;


ApiIndividualApi apiInstance = new ApiIndividualApi();
File file = new File("/path/to/file.txt"); // File | Zip archive consisting on exported api configuration 
Boolean preserveProvider = true; // Boolean | Preserve Original Provider of the API. This is the user choice to keep or replace the API provider. 
Boolean overwrite = true; // Boolean | Whether to update the API or not. This is used when updating already existing APIs. 
try {
    apiInstance.importApiPost(file, preserveProvider, overwrite);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiIndividualApi#importApiPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| Zip archive consisting on exported api configuration  |
 **preserveProvider** | **Boolean**| Preserve Original Provider of the API. This is the user choice to keep or replace the API provider.  | [optional]
 **overwrite** | **Boolean**| Whether to update the API or not. This is used when updating already existing APIs.  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

