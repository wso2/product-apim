# ApiLifecycleApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdLifecycleHistoryGet**](ApiLifecycleApi.md#apisApiIdLifecycleHistoryGet) | **GET** /apis/{apiId}/lifecycle-history | Get Lifecycle state change history of the API.
[**apisApiIdLifecycleStateGet**](ApiLifecycleApi.md#apisApiIdLifecycleStateGet) | **GET** /apis/{apiId}/lifecycle-state | Get Lifecycle state data of the API.
[**apisApiIdLifecycleStatePendingTasksDelete**](ApiLifecycleApi.md#apisApiIdLifecycleStatePendingTasksDelete) | **DELETE** /apis/{apiId}/lifecycle-state/pending-tasks | Delete pending lifecycle state change tasks.
[**apisChangeLifecyclePost**](ApiLifecycleApi.md#apisChangeLifecyclePost) | **POST** /apis/change-lifecycle | Change API Status


<a name="apisApiIdLifecycleHistoryGet"></a>
# **apisApiIdLifecycleHistoryGet**
> LifecycleHistoryDTO apisApiIdLifecycleHistoryGet(apiId, ifNoneMatch)

Get Lifecycle state change history of the API.

This operation can be used to retrieve Lifecycle state change history of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiLifecycleApi apiInstance = new ApiLifecycleApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    LifecycleHistoryDTO result = apiInstance.apisApiIdLifecycleHistoryGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiLifecycleApi#apisApiIdLifecycleHistoryGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdLifecycleStateGet"></a>
# **apisApiIdLifecycleStateGet**
> LifecycleStateDTO apisApiIdLifecycleStateGet(apiId, ifNoneMatch)

Get Lifecycle state data of the API.

This operation can be used to retrieve Lifecycle state data of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiLifecycleApi apiInstance = new ApiLifecycleApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    LifecycleStateDTO result = apiInstance.apisApiIdLifecycleStateGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiLifecycleApi#apisApiIdLifecycleStateGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdLifecycleStatePendingTasksDelete"></a>
# **apisApiIdLifecycleStatePendingTasksDelete**
> apisApiIdLifecycleStatePendingTasksDelete(apiId)

Delete pending lifecycle state change tasks.

This operation can be used to remove pending lifecycle state change requests that are in pending state 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiLifecycleApi apiInstance = new ApiLifecycleApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    apiInstance.apisApiIdLifecycleStatePendingTasksDelete(apiId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiLifecycleApi#apisApiIdLifecycleStatePendingTasksDelete");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisChangeLifecyclePost"></a>
# **apisChangeLifecyclePost**
> WorkflowResponseDTO apisChangeLifecyclePost(action, apiId, lifecycleChecklist, ifMatch)

Change API Status

This operation is used to change the lifecycle of an API. Eg: Publish an API which is in &#x60;CREATED&#x60; state. In order to change the lifecycle, we need to provide the lifecycle &#x60;action&#x60; as a query parameter.  For example, to Publish an API, &#x60;action&#x60; should be &#x60;Publish&#x60;. Note that the &#x60;Re-publish&#x60; action is available only after calling &#x60;Block&#x60;.  Some actions supports providing additional paramters which should be provided as &#x60;lifecycleChecklist&#x60; parameter. Please see parameters table for more information. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiLifecycleApi apiInstance = new ApiLifecycleApi();
String action = "action_example"; // String | The action to demote or promote the state of the API.  Supported actions are [ **Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Block, Deprecate, Re-Publish, Retire **] 
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
String lifecycleChecklist = "lifecycleChecklist_example"; // String |  Supported checklist items are as follows. 1. **Deprecate Old Versions**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Require Re-Subscription**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version.  You can specify additional checklist items by using an **\"attribute:\"** modifier.  Eg: \"Deprecate Old Versions:true\" will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \"attribute1:true, attribute2:false\" format.  **Sample CURL :**  curl -k -H \"Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\" -X POST \"https://localhost:9443/api/am/publisher/v1/apis/change-lifecycle?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&action=Publish&lifecycleChecklist=Deprecate Old Versions:true,Require Re-Subscription:true\" 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    WorkflowResponseDTO result = apiInstance.apisChangeLifecyclePost(action, apiId, lifecycleChecklist, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiLifecycleApi#apisChangeLifecyclePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **action** | **String**| The action to demote or promote the state of the API.  Supported actions are [ **Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Block, Deprecate, Re-Publish, Retire **]  | [enum: Publish, Deploy as a Prototype, Demote to Created, Demote to Prototyped, Block, Deprecate, Re-Publish, Retire]
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **lifecycleChecklist** | **String**|  Supported checklist items are as follows. 1. **Deprecate Old Versions**: Setting this to true will deprecate older versions of a particular API when it is promoted to Published state from Created state. 2. **Require Re-Subscription**: If you set this to true, users need to re subscribe to the API although they may have subscribed to an older version.  You can specify additional checklist items by using an **\&quot;attribute:\&quot;** modifier.  Eg: \&quot;Deprecate Old Versions:true\&quot; will deprecate older versions of a particular API when it is promoted to Published state from Created state. Multiple checklist items can be given in \&quot;attribute1:true, attribute2:false\&quot; format.  **Sample CURL :**  curl -k -H \&quot;Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v1/apis/change-lifecycle?apiId&#x3D;890a4f4d-09eb-4877-a323-57f6ce2ed79b&amp;action&#x3D;Publish&amp;lifecycleChecklist&#x3D;Deprecate Old Versions:true,Require Re-Subscription:true\&quot;  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**WorkflowResponseDTO**](WorkflowResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

