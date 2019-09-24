# ScopeCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdScopesGet**](ScopeCollectionApi.md#apisApiIdScopesGet) | **GET** /apis/{apiId}/scopes | Get a list of scopes of an API
[**apisApiIdScopesPost**](ScopeCollectionApi.md#apisApiIdScopesPost) | **POST** /apis/{apiId}/scopes | Add a new scope to an API


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
//import org.wso2.am.integration.clients.publisher.api.v1.ScopeCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopeCollectionApi apiInstance = new ScopeCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ScopeListDTO result = apiInstance.apisApiIdScopesGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopeCollectionApi#apisApiIdScopesGet");
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
//import org.wso2.am.integration.clients.publisher.api.v1.ScopeCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopeCollectionApi apiInstance = new ScopeCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
ScopeDTO body = new ScopeDTO(); // ScopeDTO | Scope object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ScopeDTO result = apiInstance.apisApiIdScopesPost(apiId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopeCollectionApi#apisApiIdScopesPost");
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

