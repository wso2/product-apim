# SubscriptionsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAdditionalInfoOfAPISubscriptions**](SubscriptionsApi.md#getAdditionalInfoOfAPISubscriptions) | **GET** /subscriptions/{apiId}/additionalInfo | Get Additional Information of subscriptions attached to an API.
[**subscriptionsGet**](SubscriptionsApi.md#subscriptionsGet) | **GET** /subscriptions | Get All Subscriptions 
[**subscriptionsMultiplePost**](SubscriptionsApi.md#subscriptionsMultiplePost) | **POST** /subscriptions/multiple | Add New Subscriptions 
[**subscriptionsPost**](SubscriptionsApi.md#subscriptionsPost) | **POST** /subscriptions | Add a New Subscription 
[**subscriptionsSubscriptionIdDelete**](SubscriptionsApi.md#subscriptionsSubscriptionIdDelete) | **DELETE** /subscriptions/{subscriptionId} | Remove a Subscription 
[**subscriptionsSubscriptionIdGet**](SubscriptionsApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get Details of a Subscription 
[**subscriptionsSubscriptionIdPut**](SubscriptionsApi.md#subscriptionsSubscriptionIdPut) | **PUT** /subscriptions/{subscriptionId} | Update Existing Subscription 


<a name="getAdditionalInfoOfAPISubscriptions"></a>
# **getAdditionalInfoOfAPISubscriptions**
> AdditionalSubscriptionInfoListDTO getAdditionalInfoOfAPISubscriptions(apiId, groupId, xWSO2Tenant, offset, limit, ifNoneMatch)

Get Additional Information of subscriptions attached to an API.

This operation can be used to retrieve all additional Information of subscriptions attached to an API by providing the API id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String groupId = "groupId_example"; // String | Application Group Id 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      AdditionalSubscriptionInfoListDTO result = apiInstance.getAdditionalInfoOfAPISubscriptions(apiId, groupId, xWSO2Tenant, offset, limit, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#getAdditionalInfoOfAPISubscriptions");
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
 **groupId** | **String**| Application Group Id  | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**AdditionalSubscriptionInfoListDTO**](AdditionalSubscriptionInfoListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Types and fields returned successfully.  |  * Content-Type - The content of the body.  <br>  |
**404** | Not Found. Retrieving types and fields failed.  |  -  |

<a name="subscriptionsGet"></a>
# **subscriptionsGet**
> SubscriptionListDTO subscriptionsGet(apiId, applicationId, groupId, xWSO2Tenant, offset, limit, ifNoneMatch)

Get All Subscriptions 

This operation can be used to retrieve a list of subscriptions of the user associated with the provided access token. This operation is capable of  1. Retrieving applications which are subscibed to a specific API. &#x60;GET https://localhost:9443/api/am/devportal/v2/subscriptions?apiId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60;  2. Retrieving APIs which are subscribed by a specific application. &#x60;GET https://localhost:9443/api/am/devportal/v2/subscriptions?applicationId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60;  **IMPORTANT:** * It is mandatory to provide either **apiId** or **applicationId**. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String applicationId = "applicationId_example"; // String | **Application Identifier** consisting of the UUID of the Application. 
    String groupId = "groupId_example"; // String | Application Group Id 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      SubscriptionListDTO result = apiInstance.subscriptionsGet(apiId, applicationId, groupId, xWSO2Tenant, offset, limit, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#subscriptionsGet");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  | [optional]
 **applicationId** | **String**| **Application Identifier** consisting of the UUID of the Application.  | [optional]
 **groupId** | **String**| Application Group Id  | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**SubscriptionListDTO**](SubscriptionListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Subscription list returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="subscriptionsMultiplePost"></a>
# **subscriptionsMultiplePost**
> List&lt;SubscriptionDTO&gt; subscriptionsMultiplePost(subscriptionDTO, xWSO2Tenant)

Add New Subscriptions 

This operation can be used to add a new subscriptions providing the ids of the APIs and the applications. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    List<SubscriptionDTO> subscriptionDTO = Arrays.asList(); // List<SubscriptionDTO> | Subscription objects that should to be added 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      List<SubscriptionDTO> result = apiInstance.subscriptionsMultiplePost(subscriptionDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#subscriptionsMultiplePost");
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
 **subscriptionDTO** | [**List&lt;SubscriptionDTO&gt;**](SubscriptionDTO.md)| Subscription objects that should to be added  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**List&lt;SubscriptionDTO&gt;**](SubscriptionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with the newly created objects as entity in the body.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="subscriptionsPost"></a>
# **subscriptionsPost**
> SubscriptionDTO subscriptionsPost(subscriptionDTO, xWSO2Tenant)

Add a New Subscription 

This operation can be used to add a new subscription providing the id of the API and the application. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    SubscriptionDTO subscriptionDTO = new SubscriptionDTO(); // SubscriptionDTO | Subscription object that should to be added 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      SubscriptionDTO result = apiInstance.subscriptionsPost(subscriptionDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#subscriptionsPost");
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
 **subscriptionDTO** | [**SubscriptionDTO**](SubscriptionDTO.md)| Subscription object that should to be added  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**SubscriptionDTO**](SubscriptionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  * Location - Location to the newly created subscription.  <br>  |
**202** | Accepted. The request has been accepted.  |  * Location - Location of the newly created subscription.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="subscriptionsSubscriptionIdDelete"></a>
# **subscriptionsSubscriptionIdDelete**
> subscriptionsSubscriptionIdDelete(subscriptionId, ifMatch)

Remove a Subscription 

This operation can be used to remove a subscription. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.subscriptionsSubscriptionIdDelete(subscriptionId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#subscriptionsSubscriptionIdDelete");
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
 **subscriptionId** | **String**| Subscription Id  |
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
**202** | Accepted. The request has been accepted.  |  * Location - Location of the existing subscription.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="subscriptionsSubscriptionIdGet"></a>
# **subscriptionsSubscriptionIdGet**
> SubscriptionDTO subscriptionsSubscriptionIdGet(subscriptionId, ifNoneMatch)

Get Details of a Subscription 

This operation can be used to get details of a single subscription. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      SubscriptionDTO result = apiInstance.subscriptionsSubscriptionIdGet(subscriptionId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#subscriptionsSubscriptionIdGet");
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
 **subscriptionId** | **String**| Subscription Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**SubscriptionDTO**](SubscriptionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Subscription returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests. <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional reuquests. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="subscriptionsSubscriptionIdPut"></a>
# **subscriptionsSubscriptionIdPut**
> SubscriptionDTO subscriptionsSubscriptionIdPut(subscriptionId, subscriptionDTO, xWSO2Tenant)

Update Existing Subscription 

This operation can be used to update a subscription providing the subscription id, api id, application Id, status and updated throttling policy tier. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    SubscriptionsApi apiInstance = new SubscriptionsApi(defaultClient);
    String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
    SubscriptionDTO subscriptionDTO = new SubscriptionDTO(); // SubscriptionDTO | Subscription object that should to be added 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      SubscriptionDTO result = apiInstance.subscriptionsSubscriptionIdPut(subscriptionId, subscriptionDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SubscriptionsApi#subscriptionsSubscriptionIdPut");
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
 **subscriptionId** | **String**| Subscription Id  |
 **subscriptionDTO** | [**SubscriptionDTO**](SubscriptionDTO.md)| Subscription object that should to be added  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**SubscriptionDTO**](SubscriptionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Subscription Updated. Successful response with the updated object as entity in the body. Location header contains URL of newly updates entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  * Location - Location to the updated subscription.  <br>  |
**202** | Accepted. The request has been accepted.  |  * Location - Location of the updated subscription.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. Requested Subscription does not exist.  |  -  |
**415** | Unsupported media type. The entity of the request was in a not supported format.  |  -  |

