# ApplicationsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdDelete**](ApplicationsApi.md#applicationsApplicationIdDelete) | **DELETE** /applications/{applicationId} | Remove an Application 
[**applicationsApplicationIdGet**](ApplicationsApi.md#applicationsApplicationIdGet) | **GET** /applications/{applicationId} | Get Details of an Application 
[**applicationsApplicationIdPut**](ApplicationsApi.md#applicationsApplicationIdPut) | **PUT** /applications/{applicationId} | Update an Application 
[**applicationsApplicationIdResetThrottlePolicyPost**](ApplicationsApi.md#applicationsApplicationIdResetThrottlePolicyPost) | **POST** /applications/{applicationId}/reset-throttle-policy | Reset Application-Level Throttle Policy
[**applicationsGet**](ApplicationsApi.md#applicationsGet) | **GET** /applications | Retrieve/Search Applications 
[**applicationsPost**](ApplicationsApi.md#applicationsPost) | **POST** /applications | Create a New Application 


<a name="applicationsApplicationIdDelete"></a>
# **applicationsApplicationIdDelete**
> applicationsApplicationIdDelete(applicationId, ifMatch)

Remove an Application 

This operation can be used to remove an application specifying its id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationsApi apiInstance = new ApplicationsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.applicationsApplicationIdDelete(applicationId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdDelete");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Resource successfully deleted.  |  -  |
**202** | Accepted. The request has been accepted.  |  * Location - Location of the existing Application.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdGet"></a>
# **applicationsApplicationIdGet**
> ApplicationDTO applicationsApplicationIdGet(applicationId, ifNoneMatch, xWSO2Tenant)

Get Details of an Application 

This operation can be used to retrieve details of an individual application specifying the application id in the URI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationsApi apiInstance = new ApplicationsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      ApplicationDTO result = apiInstance.applicationsApplicationIdGet(applicationId, ifNoneMatch, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdGet");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Application returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="applicationsApplicationIdPut"></a>
# **applicationsApplicationIdPut**
> ApplicationDTO applicationsApplicationIdPut(applicationId, applicationDTO, ifMatch)

Update an Application 

This operation can be used to update an application. Upon succesfull you will retrieve the updated application as the response. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationsApi apiInstance = new ApplicationsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    ApplicationDTO applicationDTO = new ApplicationDTO(); // ApplicationDTO | Application object that needs to be updated 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      ApplicationDTO result = apiInstance.applicationsApplicationIdPut(applicationId, applicationDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdPut");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **applicationDTO** | [**ApplicationDTO**](ApplicationDTO.md)| Application object that needs to be updated  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Application updated.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests.  <br>  * Location - The URL of the newly created resource.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdResetThrottlePolicyPost"></a>
# **applicationsApplicationIdResetThrottlePolicyPost**
> applicationsApplicationIdResetThrottlePolicyPost(applicationId, applicationThrottleResetDTO)

Reset Application-Level Throttle Policy

This operation can be used to reset the application-level throttle policy for a specific user. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationsApi apiInstance = new ApplicationsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    ApplicationThrottleResetDTO applicationThrottleResetDTO = new ApplicationThrottleResetDTO(); // ApplicationThrottleResetDTO | Payload for which the application-level throttle policy needs to be reset 
    try {
      apiInstance.applicationsApplicationIdResetThrottlePolicyPost(applicationId, applicationThrottleResetDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationsApi#applicationsApplicationIdResetThrottlePolicyPost");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **applicationThrottleResetDTO** | [**ApplicationThrottleResetDTO**](ApplicationThrottleResetDTO.md)| Payload for which the application-level throttle policy needs to be reset  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Application-level throttle policy reset successfully |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="applicationsGet"></a>
# **applicationsGet**
> ApplicationListDTO applicationsGet(groupId, query, sortBy, sortOrder, limit, offset, ifNoneMatch)

Retrieve/Search Applications 

This operation can be used to retrieve list of applications that is belonged to the user associated with the provided access token. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationsApi apiInstance = new ApplicationsApi(defaultClient);
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

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Application list returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="applicationsPost"></a>
# **applicationsPost**
> ApplicationDTO applicationsPost(applicationDTO)

Create a New Application 

This operation can be used to create a new application specifying the details of the application in the payload. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationsApi apiInstance = new ApplicationsApi(defaultClient);
    ApplicationDTO applicationDTO = new ApplicationDTO(); // ApplicationDTO | Application object that is to be created. 
    try {
      ApplicationDTO result = apiInstance.applicationsPost(applicationDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationsApi#applicationsPost");
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
 **applicationDTO** | [**ApplicationDTO**](ApplicationDTO.md)| Application object that is to be created.  |

### Return type

[**ApplicationDTO**](ApplicationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request  <br>  * Location - Location of the newly created Application.  <br>  |
**202** | Accepted. The request has been accepted.  |  * Location - Location of the newly created Application.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

