# ApplicationIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsImportPost**](ApplicationIndividualApi.md#applicationsImportPost) | **POST** /applications/import | Import an Application


<a name="applicationsImportPost"></a>
# **applicationsImportPost**
> ApplicationInfoDTO applicationsImportPost(file, preserveOwner, skipSubscriptions, appOwner, skipApplicationKeys, update)

Import an Application

This operation can be used to import an application. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationIndividualApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationIndividualApi apiInstance = new ApplicationIndividualApi(defaultClient);
    File file = new File("/path/to/file"); // File | Zip archive consisting of exported Application Configuration. 
    Boolean preserveOwner = true; // Boolean | Preserve Original Creator of the Application 
    Boolean skipSubscriptions = true; // Boolean | Skip importing Subscriptions of the Application 
    String appOwner = "appOwner_example"; // String | Expected Owner of the Application in the Import Environment 
    Boolean skipApplicationKeys = true; // Boolean | Skip importing Keys of the Application 
    Boolean update = true; // Boolean | Update if application exists 
    try {
      ApplicationInfoDTO result = apiInstance.applicationsImportPost(file, preserveOwner, skipSubscriptions, appOwner, skipApplicationKeys, update);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationIndividualApi#applicationsImportPost");
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
 **file** | **File**| Zip archive consisting of exported Application Configuration.  |
 **preserveOwner** | **Boolean**| Preserve Original Creator of the Application  | [optional]
 **skipSubscriptions** | **Boolean**| Skip importing Subscriptions of the Application  | [optional]
 **appOwner** | **String**| Expected Owner of the Application in the Import Environment  | [optional]
 **skipApplicationKeys** | **Boolean**| Skip importing Keys of the Application  | [optional]
 **update** | **Boolean**| Update if application exists  | [optional]

### Return type

[**ApplicationInfoDTO**](ApplicationInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with the updated object information as entity in the body.  |  * Content-Type - The content type of the body.  <br>  |
**207** | Multi Status. Partially successful response with skipped APIs information object as entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

