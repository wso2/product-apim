# ImportAndExportApplicationsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**exportApplicationsGet**](ImportAndExportApplicationsApi.md#exportApplicationsGet) | **GET** /export/applications | Export details related to an Application.
[**importApplicationsPost**](ImportAndExportApplicationsApi.md#importApplicationsPost) | **POST** /import/applications | Imports an Application.
[**importApplicationsPut**](ImportAndExportApplicationsApi.md#importApplicationsPut) | **PUT** /import/applications | Imports an Updates an Application.


<a name="exportApplicationsGet"></a>
# **exportApplicationsGet**
> File exportApplicationsGet(appId)

Export details related to an Application.

This operation can be used to export details related to a perticular application. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ImportAndExportApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ImportAndExportApplicationsApi apiInstance = new ImportAndExportApplicationsApi();
String appId = "appId_example"; // String | Application Search Query 
try {
    File result = apiInstance.exportApplicationsGet(appId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImportAndExportApplicationsApi#exportApplicationsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **appId** | **String**| Application Search Query  |

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/zip

<a name="importApplicationsPost"></a>
# **importApplicationsPost**
> ApplicationDTO importApplicationsPost(file)

Imports an Application.

This operation can be used to import an existing Application. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ImportAndExportApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ImportAndExportApplicationsApi apiInstance = new ImportAndExportApplicationsApi();
File file = new File("/path/to/file.txt"); // File | Zip archive consisting on exported application configuration 
try {
    ApplicationDTO result = apiInstance.importApplicationsPost(file);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImportAndExportApplicationsApi#importApplicationsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| Zip archive consisting on exported application configuration  |

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="importApplicationsPut"></a>
# **importApplicationsPut**
> ApplicationDTO importApplicationsPut(file)

Imports an Updates an Application.

This operation can be used to import an existing Application. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ImportAndExportApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ImportAndExportApplicationsApi apiInstance = new ImportAndExportApplicationsApi();
File file = new File("/path/to/file.txt"); // File | Zip archive consisting on exported application configuration 
try {
    ApplicationDTO result = apiInstance.importApplicationsPut(file);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ImportAndExportApplicationsApi#importApplicationsPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| Zip archive consisting on exported application configuration  |

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

