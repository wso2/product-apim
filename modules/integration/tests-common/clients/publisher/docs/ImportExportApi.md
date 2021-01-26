# ImportExportApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**exportAPI**](ImportExportApi.md#exportAPI) | **GET** /apis/export | Export an API
[**exportAPIProduct**](ImportExportApi.md#exportAPIProduct) | **GET** /api-products/export | Export an API Product
[**importAPI**](ImportExportApi.md#importAPI) | **POST** /apis/import | Import an API
[**importAPIProduct**](ImportExportApi.md#importAPIProduct) | **POST** /api-products/import | Import an API Product


<a name="exportAPI"></a>
# **exportAPI**
> File exportAPI(apiId, name, version, providerName, format, preserveStatus)

Export an API

This operation can be used to export the details of a particular API as a zip file. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ImportExportApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ImportExportApi apiInstance = new ImportExportApi(defaultClient);
    String apiId = "apiId_example"; // String | UUID of the API
    String name = "name_example"; // String | API Name 
    String version = "version_example"; // String | Version of the API 
    String providerName = "providerName_example"; // String | Provider name of the API 
    String format = "format_example"; // String | Format of output documents. Can be YAML or JSON. 
    Boolean preserveStatus = true; // Boolean | Preserve API Status on export 
    try {
      File result = apiInstance.exportAPI(apiId, name, version, providerName, format, preserveStatus);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ImportExportApi#exportAPI");
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

 - **Content-Type**: Not defined
 - **Accept**: application/zip, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Export Successful.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="exportAPIProduct"></a>
# **exportAPIProduct**
> File exportAPIProduct(name, version, providerName, format, preserveStatus)

Export an API Product

This operation can be used to export the details of a particular API Product as a zip file. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ImportExportApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ImportExportApi apiInstance = new ImportExportApi(defaultClient);
    String name = "name_example"; // String | API Product Name 
    String version = "version_example"; // String | Version of the API Product 
    String providerName = "providerName_example"; // String | Provider name of the API Product 
    String format = "format_example"; // String | Format of output documents. Can be YAML or JSON. 
    Boolean preserveStatus = true; // Boolean | Preserve API Product Status on export 
    try {
      File result = apiInstance.exportAPIProduct(name, version, providerName, format, preserveStatus);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ImportExportApi#exportAPIProduct");
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
 **name** | **String**| API Product Name  | [optional]
 **version** | **String**| Version of the API Product  | [optional]
 **providerName** | **String**| Provider name of the API Product  | [optional]
 **format** | **String**| Format of output documents. Can be YAML or JSON.  | [optional] [enum: JSON, YAML]
 **preserveStatus** | **Boolean**| Preserve API Product Status on export  | [optional]

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/zip, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Export Successful.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="importAPI"></a>
# **importAPI**
> importAPI(file, preserveProvider, overwrite)

Import an API

This operation can be used to import an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ImportExportApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ImportExportApi apiInstance = new ImportExportApi(defaultClient);
    File file = new File("/path/to/file"); // File | Zip archive consisting on exported api configuration
    Boolean preserveProvider = true; // Boolean | Preserve Original Provider of the API. This is the user choice to keep or replace the API provider 
    Boolean overwrite = true; // Boolean | Whether to update the API or not. This is used when updating already existing APIs 
    try {
      apiInstance.importAPI(file, preserveProvider, overwrite);
    } catch (ApiException e) {
      System.err.println("Exception when calling ImportExportApi#importAPI");
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
 **file** | **File**| Zip archive consisting on exported api configuration |
 **preserveProvider** | **Boolean**| Preserve Original Provider of the API. This is the user choice to keep or replace the API provider  | [optional]
 **overwrite** | **Boolean**| Whether to update the API or not. This is used when updating already existing APIs  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Created. API Imported Successfully.  |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**500** | Internal Server Error. |  -  |

<a name="importAPIProduct"></a>
# **importAPIProduct**
> importAPIProduct(file, preserveProvider, importAPIs, overwriteAPIProduct, overwriteAPIs)

Import an API Product

This operation can be used to import an API Product. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ImportExportApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ImportExportApi apiInstance = new ImportExportApi(defaultClient);
    File file = new File("/path/to/file"); // File | Zip archive consisting on exported API Product configuration 
    Boolean preserveProvider = true; // Boolean | Preserve Original Provider of the API Product. This is the user choice to keep or replace the API Product provider 
    Boolean importAPIs = true; // Boolean | Whether to import the dependent APIs or not. 
    Boolean overwriteAPIProduct = true; // Boolean | Whether to update the API Product or not. This is used when updating already existing API Products. 
    Boolean overwriteAPIs = true; // Boolean | Whether to update the dependent APIs or not. This is used when updating already existing dependent APIs of an API Product. 
    try {
      apiInstance.importAPIProduct(file, preserveProvider, importAPIs, overwriteAPIProduct, overwriteAPIs);
    } catch (ApiException e) {
      System.err.println("Exception when calling ImportExportApi#importAPIProduct");
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
 **file** | **File**| Zip archive consisting on exported API Product configuration  |
 **preserveProvider** | **Boolean**| Preserve Original Provider of the API Product. This is the user choice to keep or replace the API Product provider  | [optional]
 **importAPIs** | **Boolean**| Whether to import the dependent APIs or not.  | [optional]
 **overwriteAPIProduct** | **Boolean**| Whether to update the API Product or not. This is used when updating already existing API Products.  | [optional]
 **overwriteAPIs** | **Boolean**| Whether to update the dependent APIs or not. This is used when updating already existing dependent APIs of an API Product.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Created. API Product Imported Successfully.  |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**500** | Internal Server Error. |  -  |

