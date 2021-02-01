# CommentsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addCommentToAPI**](CommentsApi.md#addCommentToAPI) | **POST** /apis/{apiId}/comments | Add an API Comment
[**deleteComment**](CommentsApi.md#deleteComment) | **DELETE** /apis/{apiId}/comments/{commentId} | Delete an API Comment
[**getAllCommentsOfAPI**](CommentsApi.md#getAllCommentsOfAPI) | **GET** /apis/{apiId}/comments | Retrieve API Comments
[**getCommentOfAPI**](CommentsApi.md#getCommentOfAPI) | **GET** /apis/{apiId}/comments/{commentId} | Get Details of an API Comment


<a name="addCommentToAPI"></a>
# **addCommentToAPI**
> CommentDTO addCommentToAPI(apiId, commentDTO)

Add an API Comment

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    CommentDTO commentDTO = new CommentDTO(); // CommentDTO | Comment object that should to be added 
    try {
      CommentDTO result = apiInstance.addCommentToAPI(apiId, commentDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#addCommentToAPI");
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
 **commentDTO** | [**CommentDTO**](CommentDTO.md)| Comment object that should to be added  |

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  * Location - Location to the newly created Comment.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="deleteComment"></a>
# **deleteComment**
> deleteComment(commentId, apiId, ifMatch)

Delete an API Comment

Remove a Comment 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String commentId = "commentId_example"; // String | Comment Id 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteComment(commentId, apiId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#deleteComment");
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
 **commentId** | **String**| Comment Id  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
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
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAllCommentsOfAPI"></a>
# **getAllCommentsOfAPI**
> CommentListDTO getAllCommentsOfAPI(apiId, xWSO2Tenant, limit, offset, includeCommenterInfo)

Retrieve API Comments

Get a list of Comments that are already added to APIs 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    try {
      CommentListDTO result = apiInstance.getAllCommentsOfAPI(apiId, xWSO2Tenant, limit, offset, includeCommenterInfo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#getAllCommentsOfAPI");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **includeCommenterInfo** | **Boolean**| Whether we need to display commentor details.  | [optional] [default to false]

### Return type

[**CommentListDTO**](CommentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Comments list is returned.  |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getCommentOfAPI"></a>
# **getCommentOfAPI**
> CommentDTO getCommentOfAPI(commentId, apiId, xWSO2Tenant, ifNoneMatch, includeCommenterInfo)

Get Details of an API Comment

Get the individual comment given by a username for a certain API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.CommentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String commentId = "commentId_example"; // String | Comment Id 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    try {
      CommentDTO result = apiInstance.getCommentOfAPI(commentId, apiId, xWSO2Tenant, ifNoneMatch, includeCommenterInfo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#getCommentOfAPI");
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
 **commentId** | **String**| Comment Id  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
 **includeCommenterInfo** | **Boolean**| Whether we need to display commentor details.  | [optional] [default to false]

### Return type

[**CommentDTO**](CommentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Comment returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

