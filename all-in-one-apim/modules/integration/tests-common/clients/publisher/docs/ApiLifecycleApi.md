# ApiLifecycleApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**changeAPILifecycle**](ApiLifecycleApi.md#changeAPILifecycle) | **POST** /apis/change-lifecycle | Change API Status
[**deleteAPILifecycleStatePendingTasks**](ApiLifecycleApi.md#deleteAPILifecycleStatePendingTasks) | **DELETE** /apis/{apiId}/lifecycle-state/pending-tasks | Delete Pending Lifecycle State Change Tasks
[**getAPILifecycleHistory**](ApiLifecycleApi.md#getAPILifecycleHistory) | **GET** /apis/{apiId}/lifecycle-history | Get Lifecycle State Change History of the API.
[**getAPILifecycleState**](ApiLifecycleApi.md#getAPILifecycleState) | **GET** /apis/{apiId}/lifecycle-state | Get Lifecycle State Data of the API.


<a name="changeAPILifecycle"></a>
# **changeAPILifecycle**
> WorkflowResponseDTO changeAPILifecycle(action, apiId, lifecycleChecklist, ifMatch)

Change API Status

This operation is used to change the lifecycle of an API. Eg: Publish an API which is in &#x60;CREATED&#x60; state. In order to change the lifecycle, we need to provide the lifecycle &#x60;action&#x60; as a query parameter.  For example, to Publish an API, &#x60;action&#x60; should be &#x60;Publish&#x60;. Note that the &#x60;Re-publish&#x60; action is available only after calling &#x60;Block&#x60;.  Some actions supports providing additional paramters which should be provided as &#x60;lifecycleChecklist&#x60; parameter. Please see parameters table for more information. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiLifecycleApi apiInstance = new ApiLifecycleApi(defaultClient);
    String action = "action_example"; // String | The action to demote or promote the state of the API.  Supported actions are [ **Publish**, **Deploy as a Prototype**, **Demote to Created**, **Block**, **Deprecate**, **Re-Publish**, **Retire** ] 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
    String lifecycleChecklist = "lifecycleChecklist_example"; // String |  Supported checklist items are as follows. 1. **Deprecate old versions after publishing the API**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Requires re-subscription when publishing the API**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version. You can specify additional checklist items by using an **\"attribute:\"** modifier. Eg: \"Deprecate old versions after publishing the API:true\" will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \"attribute1:true, attribute2:false\" format. **Sample CURL :**  curl -k -H \"Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\" -X POST \"https://localhost:9443/api/am/publisher/v4/apis/change-lifecycle?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&action=Publish&lifecycleChecklist=Deprecate%20old%20versions%20after%20publishing%20the%20API%3Atrue,Requires%20re-subscription%20when%20publishing%20the%20API%3Afalse\" 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      WorkflowResponseDTO result = apiInstance.changeAPILifecycle(action, apiId, lifecycleChecklist, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiLifecycleApi#changeAPILifecycle");
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
 **action** | **String**| The action to demote or promote the state of the API.  Supported actions are [ **Publish**, **Deploy as a Prototype**, **Demote to Created**, **Block**, **Deprecate**, **Re-Publish**, **Retire** ]  | [enum: Publish, Deploy as a Prototype, Demote to Created, Block, Deprecate, Re-Publish, Retire]
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **lifecycleChecklist** | **String**|  Supported checklist items are as follows. 1. **Deprecate old versions after publishing the API**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Requires re-subscription when publishing the API**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version. You can specify additional checklist items by using an **\&quot;attribute:\&quot;** modifier. Eg: \&quot;Deprecate old versions after publishing the API:true\&quot; will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \&quot;attribute1:true, attribute2:false\&quot; format. **Sample CURL :**  curl -k -H \&quot;Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v4/apis/change-lifecycle?apiId&#x3D;890a4f4d-09eb-4877-a323-57f6ce2ed79b&amp;action&#x3D;Publish&amp;lifecycleChecklist&#x3D;Deprecate%20old%20versions%20after%20publishing%20the%20API%3Atrue,Requires%20re-subscription%20when%20publishing%20the%20API%3Afalse\&quot;  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**WorkflowResponseDTO**](WorkflowResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Lifecycle changed successfully.  |  * ETag - Entity Tag of the changed API. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the API lifecycle has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  |
**202** | Accepted. The request has been accepted.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="deleteAPILifecycleStatePendingTasks"></a>
# **deleteAPILifecycleStatePendingTasks**
> deleteAPILifecycleStatePendingTasks(apiId)

Delete Pending Lifecycle State Change Tasks

This operation can be used to remove pending lifecycle state change requests that are in pending state 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiLifecycleApi apiInstance = new ApiLifecycleApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      apiInstance.deleteAPILifecycleStatePendingTasks(apiId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiLifecycleApi#deleteAPILifecycleStatePendingTasks");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

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
**200** | OK. Lifecycle state change pending task removed successfully.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getAPILifecycleHistory"></a>
# **getAPILifecycleHistory**
> LifecycleHistoryDTO getAPILifecycleHistory(apiId, ifNoneMatch)

Get Lifecycle State Change History of the API.

This operation can be used to retrieve Lifecycle state change history of the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiLifecycleApi apiInstance = new ApiLifecycleApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      LifecycleHistoryDTO result = apiInstance.getAPILifecycleHistory(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiLifecycleApi#getAPILifecycleHistory");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**LifecycleHistoryDTO**](LifecycleHistoryDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Lifecycle state change history returned successfully.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAPILifecycleState"></a>
# **getAPILifecycleState**
> LifecycleStateDTO getAPILifecycleState(apiId, ifNoneMatch)

Get Lifecycle State Data of the API.

This operation can be used to retrieve Lifecycle state data of the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiLifecycleApi apiInstance = new ApiLifecycleApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      LifecycleStateDTO result = apiInstance.getAPILifecycleState(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiLifecycleApi#getAPILifecycleState");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**LifecycleStateDTO**](LifecycleStateDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Lifecycle state data returned successfully.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

