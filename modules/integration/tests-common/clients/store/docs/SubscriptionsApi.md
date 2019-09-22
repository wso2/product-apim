# SubscriptionsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**subscriptionsGet**](SubscriptionsApi.md#subscriptionsGet) | **GET** /subscriptions | Get all subscriptions 
[**subscriptionsMultiplePost**](SubscriptionsApi.md#subscriptionsMultiplePost) | **POST** /subscriptions/multiple | Add new subscriptions 
[**subscriptionsPost**](SubscriptionsApi.md#subscriptionsPost) | **POST** /subscriptions | Add a new subscription 
[**subscriptionsSubscriptionIdDelete**](SubscriptionsApi.md#subscriptionsSubscriptionIdDelete) | **DELETE** /subscriptions/{subscriptionId} | Remove a subscription 
[**subscriptionsSubscriptionIdGet**](SubscriptionsApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get details of a subscription 


<a name="subscriptionsGet"></a>
# **subscriptionsGet**
> SubscriptionListDTO subscriptionsGet(apiId, applicationId, apiType, groupId, offset, limit, ifNoneMatch)

Get all subscriptions 

This operation can be used to retrieve a list of subscriptions of the user associated with the provided access token. This operation is capable of  1. Retrieving applications which are subscibed to a specific API. &#x60;GET https://localhost:9443/api/am/store/v1.0/subscriptions?apiId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60;  2. Retrieving APIs which are subscribed by a specific application. &#x60;GET https://localhost:9443/api/am/store/v1.0/subscriptions?applicationId&#x3D;c43a325c-260b-4302-81cb-768eafaa3aed&#x60;  **IMPORTANT:** * It is mandatory to provide either **apiId** or **applicationId**. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String applicationId = "applicationId_example"; // String | **Application Identifier** consisting of the UUID of the Application. 
String apiType = "apiType_example"; // String | **API TYPE** Identifies the type API(API or API_PRODUCT). 
String groupId = "groupId_example"; // String | Application Group Id 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    SubscriptionListDTO result = apiInstance.subscriptionsGet(apiId, applicationId, apiType, groupId, offset, limit, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  | [optional]
 **applicationId** | **String**| **Application Identifier** consisting of the UUID of the Application.  | [optional]
 **apiType** | **String**| **API TYPE** Identifies the type API(API or API_PRODUCT).  | [optional]
 **groupId** | **String**| Application Group Id  | [optional]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**SubscriptionListDTO**](SubscriptionListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsMultiplePost"></a>
# **subscriptionsMultiplePost**
> List&lt;SubscriptionDTO&gt; subscriptionsMultiplePost(body)

Add new subscriptions 

This operation can be used to add a new subscriptions providing the ids of the APIs and the applications. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
List<SubscriptionDTO> body = Arrays.asList(new SubscriptionDTO()); // List<SubscriptionDTO> | Subscription objects that should to be added 
try {
    List<SubscriptionDTO> result = apiInstance.subscriptionsMultiplePost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsMultiplePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**List&lt;SubscriptionDTO&gt;**](SubscriptionDTO.md)| Subscription objects that should to be added  |

### Return type

[**List&lt;SubscriptionDTO&gt;**](SubscriptionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsPost"></a>
# **subscriptionsPost**
> SubscriptionDTO subscriptionsPost(body)

Add a new subscription 

This operation can be used to add a new subscription providing the id of the API and the application. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
SubscriptionDTO body = new SubscriptionDTO(); // SubscriptionDTO | Subscription object that should to be added 
try {
    SubscriptionDTO result = apiInstance.subscriptionsPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**SubscriptionDTO**](SubscriptionDTO.md)| Subscription object that should to be added  |

### Return type

[**SubscriptionDTO**](SubscriptionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsSubscriptionIdDelete"></a>
# **subscriptionsSubscriptionIdDelete**
> subscriptionsSubscriptionIdDelete(subscriptionId, ifMatch)

Remove a subscription 

This operation can be used to remove a subscription. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.subscriptionsSubscriptionIdDelete(subscriptionId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsSubscriptionIdDelete");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscriptionsSubscriptionIdGet"></a>
# **subscriptionsSubscriptionIdGet**
> SubscriptionDTO subscriptionsSubscriptionIdGet(subscriptionId, ifNoneMatch)

Get details of a subscription 

This operation can be used to get details of a single subscription. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SubscriptionsApi apiInstance = new SubscriptionsApi();
String subscriptionId = "subscriptionId_example"; // String | Subscription Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    SubscriptionDTO result = apiInstance.subscriptionsSubscriptionIdGet(subscriptionId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SubscriptionsApi#subscriptionsSubscriptionIdGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

