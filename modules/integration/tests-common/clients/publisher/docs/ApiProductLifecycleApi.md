# ApiProductLifecycleApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**changeAPIProductLifecycle**](ApiProductLifecycleApi.md#changeAPIProductLifecycle) | **POST** /api-products/change-lifecycle | Change API Product LC Status
[**deleteAPIProductLifecycleStatePendingTasks**](ApiProductLifecycleApi.md#deleteAPIProductLifecycleStatePendingTasks) | **DELETE** /api-products/{apiProductId}/lifecycle-state/pending-tasks | Delete Pending Lifecycle State Change Tasks
[**getAPIProductLifecycleHistory**](ApiProductLifecycleApi.md#getAPIProductLifecycleHistory) | **GET** /api-products/{apiProductId}/lifecycle-history | Get Lifecycle State Change History of the API Products.
[**getAPIProductLifecycleState**](ApiProductLifecycleApi.md#getAPIProductLifecycleState) | **GET** /api-products/{apiProductId}/lifecycle-state | Get Lifecycle State Data of the API Product.


<a name="changeAPIProductLifecycle"></a>
# **changeAPIProductLifecycle**
> WorkflowResponseDTO changeAPIProductLifecycle(action, apiProductId, lifecycleChecklist, ifMatch)

Change API Product LC Status

This operation is used to change the lifecycle of an API Product. Eg: Publish an API Product which is in &#x60;CREATED&#x60; state. In order to change the lifecycle, we need to provide the lifecycle &#x60;action&#x60; as a query parameter.  For example, to Publish an API Product, &#x60;action&#x60; should be &#x60;Publish&#x60;. Note that the &#x60;Re-publish&#x60; action is available only after calling &#x60;Block&#x60;.  Some actions supports providing additional paramters which should be provided as &#x60;lifecycleChecklist&#x60; parameter. Please see parameters table for more information. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductLifecycleApi apiInstance = new ApiProductLifecycleApi(defaultClient);
    String action = "action_example"; // String | The action to demote or promote the state of the API Product.  Supported actions are [ **Publish**, **Deploy as a Prototype**, **Demote to Created**, **Block**, **Deprecate**, **Re-Publish**, **Retire** ] 
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. The combination of the provider, name and the version of the API Product is also accepted as a valid API Product ID. Should be formatted as **provider-name-version**. 
    String lifecycleChecklist = "lifecycleChecklist_example"; // String | Supported checklist items are as follows. 1. **Deprecate old versions after publishing the API**: Setting this to true will deprecate older versions of a particular API Product when it is promoted to Published state from Created state. 2. **Requires re-subscription when publishing the API**: If you set this to true, users need to re subscribe to the API Products although they may have subscribed to an older version. You can specify additional checklist items by using an **\"attribute:\"** modifier. Eg: \"Deprecate old versions after publishing the API:true\" will deprecate older versions of a particular API Product when it is promoted to Published state from Created state. Multiple checklist items can be given in \"attribute1:true, attribute2:false\" format. **Sample CURL :**  curl -k -H \"Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\" -X POST \"https://localhost:9443/api/am/publisher/v4/api-products/change-lifecycle?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&action=Publish&lifecycleChecklist=Deprecate%20old%20versions%20after%20publishing%20the%20API%3Atrue,Requires%20re-subscription%20when%20publishing%20the%20API%3Afalse\" 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      WorkflowResponseDTO result = apiInstance.changeAPIProductLifecycle(action, apiProductId, lifecycleChecklist, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductLifecycleApi#changeAPIProductLifecycle");
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
 **action** | **String**| The action to demote or promote the state of the API Product.  Supported actions are [ **Publish**, **Deploy as a Prototype**, **Demote to Created**, **Block**, **Deprecate**, **Re-Publish**, **Retire** ]  | [enum: Publish, Deploy as a Prototype, Demote to Created, Block, Deprecate, Re-Publish, Retire]
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. The combination of the provider, name and the version of the API Product is also accepted as a valid API Product ID. Should be formatted as **provider-name-version**.  |
 **lifecycleChecklist** | **String**| Supported checklist items are as follows. 1. **Deprecate old versions after publishing the API**: Setting this to true will deprecate older versions of a particular API Product when it is promoted to Published state from Created state. 2. **Requires re-subscription when publishing the API**: If you set this to true, users need to re subscribe to the API Products although they may have subscribed to an older version. You can specify additional checklist items by using an **\&quot;attribute:\&quot;** modifier. Eg: \&quot;Deprecate old versions after publishing the API:true\&quot; will deprecate older versions of a particular API Product when it is promoted to Published state from Created state. Multiple checklist items can be given in \&quot;attribute1:true, attribute2:false\&quot; format. **Sample CURL :**  curl -k -H \&quot;Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v4/api-products/change-lifecycle?apiId&#x3D;890a4f4d-09eb-4877-a323-57f6ce2ed79b&amp;action&#x3D;Publish&amp;lifecycleChecklist&#x3D;Deprecate%20old%20versions%20after%20publishing%20the%20API%3Atrue,Requires%20re-subscription%20when%20publishing%20the%20API%3Afalse\&quot;  | [optional]
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
**200** | OK. Lifecycle changed successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteAPIProductLifecycleStatePendingTasks"></a>
# **deleteAPIProductLifecycleStatePendingTasks**
> deleteAPIProductLifecycleStatePendingTasks(apiProductId)

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
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductLifecycleApi apiInstance = new ApiProductLifecycleApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    try {
      apiInstance.deleteAPIProductLifecycleStatePendingTasks(apiProductId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductLifecycleApi#deleteAPIProductLifecycleStatePendingTasks");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |

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
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAPIProductLifecycleHistory"></a>
# **getAPIProductLifecycleHistory**
> LifecycleHistoryDTO getAPIProductLifecycleHistory(apiProductId, ifNoneMatch)

Get Lifecycle State Change History of the API Products.

This operation can be used to retrieve Lifecycle state change history of the API Products. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductLifecycleApi apiInstance = new ApiProductLifecycleApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      LifecycleHistoryDTO result = apiInstance.getAPIProductLifecycleHistory(apiProductId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductLifecycleApi#getAPIProductLifecycleHistory");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
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
**200** | OK. Lifecycle state change history returned successfully.  |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAPIProductLifecycleState"></a>
# **getAPIProductLifecycleState**
> LifecycleStateDTO getAPIProductLifecycleState(apiProductId, ifNoneMatch)

Get Lifecycle State Data of the API Product.

This operation can be used to retrieve Lifecycle state data of the API Product. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductLifecycleApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductLifecycleApi apiInstance = new ApiProductLifecycleApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      LifecycleStateDTO result = apiInstance.getAPIProductLifecycleState(apiProductId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductLifecycleApi#getAPIProductLifecycleState");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
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
**200** | OK. Lifecycle state data returned successfully.  |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

