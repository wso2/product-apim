# ApplicationIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**exportApplicationsGet**](ApplicationIndividualApi.md#exportApplicationsGet) | **GET** /export/applications | Export an Application
[**importApplicationsPost**](ApplicationIndividualApi.md#importApplicationsPost) | **POST** /import/applications | Import an Application


<a name="exportApplicationsGet"></a>
# **exportApplicationsGet**
> File exportApplicationsGet(appName, appOwner, withKeys)

Export an Application

This operation can be used to export the details of a particular Application as a zip file. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String appName = "appName_example"; // String | Application Name 
String appOwner = "appOwner_example"; // String | Owner of the Application 
Boolean withKeys = true; // Boolean | Export application keys 
try {
    File result = apiInstance.exportApplicationsGet(appName, appOwner, withKeys);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#exportApplicationsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **appName** | **String**| Application Name  |
 **appOwner** | **String**| Owner of the Application  |
 **withKeys** | **Boolean**| Export application keys  | [optional]

### Return type

[**File**](File.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/zip

<a name="importApplicationsPost"></a>
# **importApplicationsPost**
> ApplicationInfoDTO importApplicationsPost(file, preserveOwner, skipSubscriptions, appOwner, skipApplicationKeys, update)

Import an Application

This operation can be used to import an Application. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
File file = new File("/path/to/file.txt"); // File | Zip archive consisting of exported Application Configuration. 
Boolean preserveOwner = true; // Boolean | Preserve Original Creator of the Application 
Boolean skipSubscriptions = true; // Boolean | Skip importing Subscriptions of the Application 
String appOwner = "appOwner_example"; // String | Expected Owner of the Application in the Import Environment 
Boolean skipApplicationKeys = true; // Boolean | Skip importing Keys of the Application 
Boolean update = true; // Boolean | Update if application exists 
try {
    ApplicationInfoDTO result = apiInstance.importApplicationsPost(file, preserveOwner, skipSubscriptions, appOwner, skipApplicationKeys, update);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#importApplicationsPost");
    e.printStackTrace();
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

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

