# ImportExportApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsExportGet**](ImportExportApi.md#applicationsExportGet) | **GET** /applications/export | Export an Application


<a name="applicationsExportGet"></a>
# **applicationsExportGet**
> File applicationsExportGet(appName, appOwner, withKeys, format)

Export an Application

This operation can be used to export the details of a particular application as a zip file. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ImportExportApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ImportExportApi apiInstance = new ImportExportApi(defaultClient);
    String appName = "appName_example"; // String | Application Name 
    String appOwner = "appOwner_example"; // String | Owner of the Application 
    Boolean withKeys = true; // Boolean | Export application keys 
    String format = "format_example"; // String | Format of output documents. Can be YAML or JSON. 
    try {
      File result = apiInstance.applicationsExportGet(appName, appOwner, withKeys, format);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ImportExportApi#applicationsExportGet");
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
 **appName** | **String**| Application Name  |
 **appOwner** | **String**| Owner of the Application  |
 **withKeys** | **Boolean**| Export application keys  | [optional]
 **format** | **String**| Format of output documents. Can be YAML or JSON.  | [optional] [enum: JSON, YAML]

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
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

