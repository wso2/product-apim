# ApiScopesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdScopesGet**](ApiScopesApi.md#apisApiIdScopesGet) | **GET** /apis/{apiId}/scopes | Get a list of scopes of an API
[**apisApiIdScopesNameDelete**](ApiScopesApi.md#apisApiIdScopesNameDelete) | **DELETE** /apis/{apiId}/scopes/{name} | Delete a scope of an API
[**apisApiIdScopesNameGet**](ApiScopesApi.md#apisApiIdScopesNameGet) | **GET** /apis/{apiId}/scopes/{name} | Get a scope of an API
[**apisApiIdScopesNamePut**](ApiScopesApi.md#apisApiIdScopesNamePut) | **PUT** /apis/{apiId}/scopes/{name} | Update a Scope of an API
[**apisApiIdScopesPost**](ApiScopesApi.md#apisApiIdScopesPost) | **POST** /apis/{apiId}/scopes | Add a new scope to an API


<a name="apisApiIdScopesGet"></a>
# **apisApiIdScopesGet**
> ScopeListDTO apisApiIdScopesGet(apiId, ifNoneMatch)

Get a list of scopes of an API

This operation can be used to retrieve a list of scopes belonging to an API by providing the id of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiScopesApi apiInstance = new ApiScopesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ScopeListDTO result = apiInstance.apisApiIdScopesGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiScopesApi#apisApiIdScopesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ScopeListDTO**](ScopeListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdScopesNameDelete"></a>
# **apisApiIdScopesNameDelete**
> apisApiIdScopesNameDelete(apiId, name, ifMatch)

Delete a scope of an API

This operation can be used to delete a scope associated with an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiScopesApi apiInstance = new ApiScopesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String name = "name_example"; // String | Scope name 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdScopesNameDelete(apiId, name, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiScopesApi#apisApiIdScopesNameDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **name** | **String**| Scope name  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdScopesNameGet"></a>
# **apisApiIdScopesNameGet**
> ScopeDTO apisApiIdScopesNameGet(apiId, name, ifNoneMatch)

Get a scope of an API

This operation can be used to retrieve a particular scope&#39;s metadata associated with an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiScopesApi apiInstance = new ApiScopesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String name = "name_example"; // String | Scope name 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ScopeDTO result = apiInstance.apisApiIdScopesNameGet(apiId, name, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiScopesApi#apisApiIdScopesNameGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **name** | **String**| Scope name  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdScopesNamePut"></a>
# **apisApiIdScopesNamePut**
> ScopeDTO apisApiIdScopesNamePut(apiId, name, body, ifMatch)

Update a Scope of an API

This operation can be used to update scope of an API 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiScopesApi apiInstance = new ApiScopesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String name = "name_example"; // String | Scope name 
ScopeDTO body = new ScopeDTO(); // ScopeDTO | Scope object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ScopeDTO result = apiInstance.apisApiIdScopesNamePut(apiId, name, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiScopesApi#apisApiIdScopesNamePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **name** | **String**| Scope name  |
 **body** | [**ScopeDTO**](ScopeDTO.md)| Scope object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdScopesPost"></a>
# **apisApiIdScopesPost**
> ScopeDTO apisApiIdScopesPost(apiId, body, ifMatch)

Add a new scope to an API

This operation can be used to add a new scope to an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiScopesApi apiInstance = new ApiScopesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
ScopeDTO body = new ScopeDTO(); // ScopeDTO | Scope object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ScopeDTO result = apiInstance.apisApiIdScopesPost(apiId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiScopesApi#apisApiIdScopesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**ScopeDTO**](ScopeDTO.md)| Scope object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

