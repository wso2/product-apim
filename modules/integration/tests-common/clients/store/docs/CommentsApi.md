# CommentsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdCommentsCommentIdDelete**](CommentsApi.md#apisApiIdCommentsCommentIdDelete) | **DELETE** /apis/{apiId}/comments/{commentId} | Delete an API comment
[**apisApiIdCommentsCommentIdGet**](CommentsApi.md#apisApiIdCommentsCommentIdGet) | **GET** /apis/{apiId}/comments/{commentId} | Get details of an API comment
[**apisApiIdCommentsCommentIdPut**](CommentsApi.md#apisApiIdCommentsCommentIdPut) | **PUT** /apis/{apiId}/comments/{commentId} | Update an API comment
[**apisApiIdCommentsGet**](CommentsApi.md#apisApiIdCommentsGet) | **GET** /apis/{apiId}/comments | Retrieve API comments
[**apisApiIdCommentsPost**](CommentsApi.md#apisApiIdCommentsPost) | **POST** /apis/{apiId}/comments | Add an API comment


<a name="apisApiIdCommentsCommentIdDelete"></a>
# **apisApiIdCommentsCommentIdDelete**
> apisApiIdCommentsCommentIdDelete(commentId, apiId, ifMatch)

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
    apiInstance.apisApiIdCommentsCommentIdDelete(commentId, apiId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apisApiIdCommentsCommentIdDelete");
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

<a name="apisApiIdCommentsCommentIdGet"></a>
# **apisApiIdCommentsCommentIdGet**
> CommentDTO apisApiIdCommentsCommentIdGet(commentId, apiId, ifNoneMatch)

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
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    CommentDTO result = apiInstance.apisApiIdCommentsCommentIdGet(commentId, apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apisApiIdCommentsCommentIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdCommentsCommentIdPut"></a>
# **apisApiIdCommentsCommentIdPut**
> CommentDTO apisApiIdCommentsCommentIdPut(commentId, apiId, body, ifMatch)

Update an API comment

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
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
CommentDTO body = new CommentDTO(); // CommentDTO | Comment object that needs to be updated 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    CommentDTO result = apiInstance.apisApiIdCommentsCommentIdPut(commentId, apiId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apisApiIdCommentsCommentIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **commentId** | **String**| Comment Id  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**CommentDTO**](CommentDTO.md)| Comment object that needs to be updated  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdCommentsGet"></a>
# **apisApiIdCommentsGet**
> CommentListDTO apisApiIdCommentsGet(apiId, limit, offset)

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
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    CommentListDTO result = apiInstance.apisApiIdCommentsGet(apiId, limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apisApiIdCommentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**CommentListDTO**](CommentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdCommentsPost"></a>
# **apisApiIdCommentsPost**
> CommentDTO apisApiIdCommentsPost(apiId, body)

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
    CommentDTO result = apiInstance.apisApiIdCommentsPost(apiId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CommentsApi#apisApiIdCommentsPost");
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

