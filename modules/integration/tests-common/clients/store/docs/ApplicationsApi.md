# ApplicationsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdDelete**](ApplicationsApi.md#applicationsApplicationIdDelete) | **DELETE** /applications/{applicationId} | Remove an application 
[**applicationsApplicationIdGet**](ApplicationsApi.md#applicationsApplicationIdGet) | **GET** /applications/{applicationId} | Get details of an application 
[**applicationsApplicationIdPut**](ApplicationsApi.md#applicationsApplicationIdPut) | **PUT** /applications/{applicationId} | Update an application 
[**applicationsGet**](ApplicationsApi.md#applicationsGet) | **GET** /applications | Retrieve/Search applications 
[**applicationsPost**](ApplicationsApi.md#applicationsPost) | **POST** /applications | Create a new application 


<a name="applicationsApplicationIdDelete"></a>
# **applicationsApplicationIdDelete**
> applicationsApplicationIdDelete(applicationId, ifMatch)

Remove an application 

This operation can be used to remove an application specifying its id. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationsApi apiInstance = new ApplicationsApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.applicationsApplicationIdDelete(applicationId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdGet"></a>
# **applicationsApplicationIdGet**
> ApplicationDTO applicationsApplicationIdGet(applicationId, ifNoneMatch)

Get details of an application 

This operation can be used to retrieve details of an individual application specifying the application id in the URI. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationsApi apiInstance = new ApplicationsApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    ApplicationDTO result = apiInstance.applicationsApplicationIdGet(applicationId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdPut"></a>
# **applicationsApplicationIdPut**
> ApplicationDTO applicationsApplicationIdPut(applicationId, body, ifMatch)

Update an application 

This operation can be used to update an application. Upon succesfull you will retrieve the updated application as the response. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationsApi apiInstance = new ApplicationsApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
ApplicationDTO body = new ApplicationDTO(); // ApplicationDTO | Application object that needs to be updated 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ApplicationDTO result = apiInstance.applicationsApplicationIdPut(applicationId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **body** | [**ApplicationDTO**](ApplicationDTO.md)| Application object that needs to be updated  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsGet"></a>
# **applicationsGet**
> ApplicationListDTO applicationsGet(groupId, query, sortBy, sortOrder, limit, offset, ifNoneMatch)

Retrieve/Search applications 

This operation can be used to retrieve list of applications that is belonged to the user associated with the provided access token. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationsApi apiInstance = new ApplicationsApi();
String groupId = "groupId_example"; // String | Application Group Id 
String query = "query_example"; // String | **Search condition**.  You can search for an application by specifying the name as \"query\" attribute.  Eg. \"app1\" will match an application if the name is exactly \"app1\".  Currently this does not support wildcards. Given name must exactly match the application name. 
String sortBy = "sortBy_example"; // String | 
String sortOrder = "sortOrder_example"; // String | 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    ApplicationListDTO result = apiInstance.applicationsGet(groupId, query, sortBy, sortOrder, limit, offset, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationsApi#applicationsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **groupId** | **String**| Application Group Id  | [optional]
 **query** | **String**| **Search condition**.  You can search for an application by specifying the name as \&quot;query\&quot; attribute.  Eg. \&quot;app1\&quot; will match an application if the name is exactly \&quot;app1\&quot;.  Currently this does not support wildcards. Given name must exactly match the application name.  | [optional]
 **sortBy** | **String**|  | [optional] [enum: name, throttlingPolicy, status]
 **sortOrder** | **String**|  | [optional] [enum: asc, desc]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**ApplicationListDTO**](ApplicationListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsPost"></a>
# **applicationsPost**
> ApplicationDTO applicationsPost(body)

Create a new application 

This operation can be used to create a new application specifying the details of the application in the payload. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationsApi apiInstance = new ApplicationsApi();
ApplicationDTO body = new ApplicationDTO(); // ApplicationDTO | Application object that is to be created. 
try {
    ApplicationDTO result = apiInstance.applicationsPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationsApi#applicationsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ApplicationDTO**](ApplicationDTO.md)| Application object that is to be created.  |

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

