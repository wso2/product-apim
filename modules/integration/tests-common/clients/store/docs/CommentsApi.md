# CommentsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addCommentToAPI**](CommentsApi.md#addCommentToAPI) | **POST** /apis/{apiId}/comments | Add an API comment
[**apiProductsApiProductIdCommentsCommentIdDelete**](CommentsApi.md#apiProductsApiProductIdCommentsCommentIdDelete) | **DELETE** /api-products/{apiProductId}/comments/{commentId} | Delete an API Product comment
[**apiProductsApiProductIdCommentsCommentIdGet**](CommentsApi.md#apiProductsApiProductIdCommentsCommentIdGet) | **GET** /api-products/{apiProductId}/comments/{commentId} | Get details of an API Product comment
[**apiProductsApiProductIdCommentsCommentIdPut**](CommentsApi.md#apiProductsApiProductIdCommentsCommentIdPut) | **PUT** /api-products/{apiProductId}/comments/{commentId} | Update an API Product comment
[**apiProductsApiProductIdCommentsGet**](CommentsApi.md#apiProductsApiProductIdCommentsGet) | **GET** /api-products/{apiProductId}/comments | Retrieve API Product comments
[**apiProductsApiProductIdCommentsPost**](CommentsApi.md#apiProductsApiProductIdCommentsPost) | **POST** /api-products/{apiProductId}/comments | Add an API Product comment
[**deleteComment**](CommentsApi.md#deleteComment) | **DELETE** /apis/{apiId}/comments/{commentId} | Delete an API comment
[**getAllCommentsOfAPI**](CommentsApi.md#getAllCommentsOfAPI) | **GET** /apis/{apiId}/comments | Retrieve API comments
[**getCommentOfAPI**](CommentsApi.md#getCommentOfAPI) | **GET** /apis/{apiId}/comments/{commentId} | Get details of an API comment


<a name="addCommentToAPI"></a>
# **addCommentToAPI**
> CommentDTO addCommentToAPI(apiId, body)

Add an API comment

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
CommentDTO body = new CommentDTO(); // CommentDTO | Comment object that should to be added 
try {
    CommentDTO result = apiInstance.addCommentToAPI(apiId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#addCommentToAPI");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**CommentDTO**](CommentDTO.md)| Comment object that should to be added  |

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdCommentsCommentIdDelete"></a>
# **apiProductsApiProductIdCommentsCommentIdDelete**
> apiProductsApiProductIdCommentsCommentIdDelete(commentId, apiProductId, ifMatch)

Delete an API Product comment

Remove a Comment 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String commentId = "commentId_example"; // String | Comment Id 
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apiProductsApiProductIdCommentsCommentIdDelete(commentId, apiProductId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apiProductsApiProductIdCommentsCommentIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdCommentsCommentIdGet"></a>
# **apiProductsApiProductIdCommentsCommentIdGet**
> CommentDTO apiProductsApiProductIdCommentsCommentIdGet(commentId, apiProductId, ifNoneMatch)

Get details of an API Product comment

Get the individual comment given by a username for a certain API Product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String commentId = "commentId_example"; // String | Comment Id 
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    CommentDTO result = apiInstance.apiProductsApiProductIdCommentsCommentIdGet(commentId, apiProductId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apiProductsApiProductIdCommentsCommentIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdCommentsCommentIdPut"></a>
# **apiProductsApiProductIdCommentsCommentIdPut**
> CommentDTO apiProductsApiProductIdCommentsCommentIdPut(commentId, apiProductId, body, ifMatch)

Update an API Product comment

Update a certain Comment 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String commentId = "commentId_example"; // String | Comment Id 
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
CommentDTO body = new CommentDTO(); // CommentDTO | Comment object that needs to be updated 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    CommentDTO result = apiInstance.apiProductsApiProductIdCommentsCommentIdPut(commentId, apiProductId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apiProductsApiProductIdCommentsCommentIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **body** | [**CommentDTO**](CommentDTO.md)| Comment object that needs to be updated  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdCommentsGet"></a>
# **apiProductsApiProductIdCommentsGet**
> CommentListDTO apiProductsApiProductIdCommentsGet(apiProductId, limit, offset)

Retrieve API Product comments

Get a list of Comments that are already added to API Products 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    CommentListDTO result = apiInstance.apiProductsApiProductIdCommentsGet(apiProductId, limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apiProductsApiProductIdCommentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**CommentListDTO**](CommentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdCommentsPost"></a>
# **apiProductsApiProductIdCommentsPost**
> CommentDTO apiProductsApiProductIdCommentsPost(apiProductId, body)

Add an API Product comment

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
CommentDTO body = new CommentDTO(); // CommentDTO | Comment object that should to be added 
try {
    CommentDTO result = apiInstance.apiProductsApiProductIdCommentsPost(apiProductId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apiProductsApiProductIdCommentsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **body** | [**CommentDTO**](CommentDTO.md)| Comment object that should to be added  |

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteComment"></a>
# **deleteComment**
> deleteComment(commentId, apiId, ifMatch)

Delete an API comment

Remove a Comment 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String commentId = "commentId_example"; // String | Comment Id 
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.deleteComment(commentId, apiId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#deleteComment");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getAllCommentsOfAPI"></a>
# **getAllCommentsOfAPI**
> CommentListDTO getAllCommentsOfAPI(apiId, xWSO2Tenant, limit, offset)

Retrieve API comments

Get a list of Comments that are already added to APIs 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    CommentListDTO result = apiInstance.getAllCommentsOfAPI(apiId, xWSO2Tenant, limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#getAllCommentsOfAPI");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**CommentListDTO**](CommentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getCommentOfAPI"></a>
# **getCommentOfAPI**
> CommentDTO getCommentOfAPI(commentId, apiId, xWSO2Tenant, ifNoneMatch)

Get details of an API comment

Get the individual comment given by a username for a certain API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CommentsApi apiInstance = new CommentsApi();
String commentId = "commentId_example"; // String | Comment Id 
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    CommentDTO result = apiInstance.getCommentOfAPI(commentId, apiId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#getCommentOfAPI");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

