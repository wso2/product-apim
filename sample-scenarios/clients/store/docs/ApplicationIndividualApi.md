# ApplicationIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdDelete**](ApplicationIndividualApi.md#applicationsApplicationIdDelete) | **DELETE** /applications/{applicationId} | Remove an application 
[**applicationsApplicationIdGet**](ApplicationIndividualApi.md#applicationsApplicationIdGet) | **GET** /applications/{applicationId} | Get details of an application 
[**applicationsApplicationIdKeysKeyTypeGet**](ApplicationIndividualApi.md#applicationsApplicationIdKeysKeyTypeGet) | **GET** /applications/{applicationId}/keys/{keyType} | Get key details of a given type 
[**applicationsApplicationIdKeysKeyTypePut**](ApplicationIndividualApi.md#applicationsApplicationIdKeysKeyTypePut) | **PUT** /applications/{applicationId}/keys/{keyType} | Update grant types and callback url of an application 
[**applicationsApplicationIdPut**](ApplicationIndividualApi.md#applicationsApplicationIdPut) | **PUT** /applications/{applicationId} | Update an application 
[**applicationsGenerateKeysPost**](ApplicationIndividualApi.md#applicationsGenerateKeysPost) | **POST** /applications/generate-keys | Generate keys for application 
[**applicationsPost**](ApplicationIndividualApi.md#applicationsPost) | **POST** /applications | Create a new application 


<a name="applicationsApplicationIdDelete"></a>
# **applicationsApplicationIdDelete**
> applicationsApplicationIdDelete(applicationId, ifMatch, ifUnmodifiedSince)

Remove an application 

This operation can be used to remove an application specifying its id. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.applicationsApplicationIdDelete(applicationId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsApplicationIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdGet"></a>
# **applicationsApplicationIdGet**
> Application applicationsApplicationIdGet(applicationId, accept, ifNoneMatch, ifModifiedSince)

Get details of an application 

This operation can be used to retrieve details of an individual application specifying the application id in the URI. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
try {
    Application result = apiInstance.applicationsApplicationIdGet(applicationId, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsApplicationIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]

### Return type

[**Application**](Application.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysKeyTypeGet"></a>
# **applicationsApplicationIdKeysKeyTypeGet**
> ApplicationKey applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId, accept)

Get key details of a given type 

This operation can be used to retrieve key details of an individual application specifying the key type in the URI. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
String groupId = "groupId_example"; // String | Application Group Id 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
try {
    ApplicationKey result = apiInstance.applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId, accept);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsApplicationIdKeysKeyTypeGet");
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
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
ApplicationKey body = new ApplicationKey(); // ApplicationKey | Grant types/Callback URL update request object 
try {
    ApplicationKey result = apiInstance.applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsApplicationIdKeysKeyTypePut");
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

<a name="applicationsApplicationIdPut"></a>
# **applicationsApplicationIdPut**
> Application applicationsApplicationIdPut(applicationId, body, contentType, ifMatch, ifUnmodifiedSince)

Update an application 

This operation can be used to update an application. Upon succesfull you will retrieve the updated application as the response. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
Application body = new Application(); // Application | Application object that needs to be updated 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    Application result = apiInstance.applicationsApplicationIdPut(applicationId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsApplicationIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **body** | [**Application**](Application.md)| Application object that needs to be updated  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**Application**](Application.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsGenerateKeysPost"></a>
# **applicationsGenerateKeysPost**
> ApplicationKey applicationsGenerateKeysPost(applicationId, body, contentType, ifMatch, ifUnmodifiedSince)

Generate keys for application 

This operation can be used to generate client Id and client secret for an application 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
ApplicationKeyGenerateRequest body = new ApplicationKeyGenerateRequest(); // ApplicationKeyGenerateRequest | Application object the keys of which are to be generated 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    ApplicationKey result = apiInstance.applicationsGenerateKeysPost(applicationId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsGenerateKeysPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **body** | [**ApplicationKeyGenerateRequest**](ApplicationKeyGenerateRequest.md)| Application object the keys of which are to be generated  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**ApplicationKey**](ApplicationKey.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsPost"></a>
# **applicationsPost**
> Application applicationsPost(body, contentType)

Create a new application 

This operation can be used to create a new application specifying the details of the application in the payload. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
Application body = new Application(); // Application | Application object that is to be created. 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    Application result = apiInstance.applicationsPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Application**](Application.md)| Application object that is to be created.  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**Application**](Application.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

