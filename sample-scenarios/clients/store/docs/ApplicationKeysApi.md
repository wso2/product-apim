# ApplicationKeysApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdKeysKeyTypeGet**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeGet) | **GET** /applications/{applicationId}/keys/{keyType} | Get key details of a given type 
[**applicationsApplicationIdKeysKeyTypePut**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypePut) | **PUT** /applications/{applicationId}/keys/{keyType} | Update grant types and callback url of an application 


<a name="applicationsApplicationIdKeysKeyTypeGet"></a>
# **applicationsApplicationIdKeysKeyTypeGet**
> ApplicationKey applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId, accept)

Get key details of a given type 

This operation can be used to retrieve key details of an individual application specifying the key type in the URI. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationKeysApi;


ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
String groupId = "groupId_example"; // String | Application Group Id 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
try {
    ApplicationKey result = apiInstance.applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId, accept);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **groupId** | **String**| Application Group Id  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]

### Return type

[**ApplicationKey**](ApplicationKey.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysKeyTypePut"></a>
# **applicationsApplicationIdKeysKeyTypePut**
> ApplicationKey applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, body)

Update grant types and callback url of an application 

This operation can be used to update grant types and callback url of an application. (Consumer Key and Consumer Secret are ignored) Upon succesfull you will retrieve the updated key details as the response. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationKeysApi;


ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
ApplicationKey body = new ApplicationKey(); // ApplicationKey | Grant types/Callback URL update request object 
try {
    ApplicationKey result = apiInstance.applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **body** | [**ApplicationKey**](ApplicationKey.md)| Grant types/Callback URL update request object  |

### Return type

[**ApplicationKey**](ApplicationKey.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

