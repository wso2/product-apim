# CommentsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addCommentToAPI**](CommentsApi.md#addCommentToAPI) | **POST** /apis/{apiId}/comments | Add an API Comment
[**addCommentToMCPServer**](CommentsApi.md#addCommentToMCPServer) | **POST** /mcp-servers/{mcpServerId}/comments | Add a MCP Server Comment
[**deleteComment**](CommentsApi.md#deleteComment) | **DELETE** /apis/{apiId}/comments/{commentId} | Delete an API Comment
[**deleteCommentOfMCPServer**](CommentsApi.md#deleteCommentOfMCPServer) | **DELETE** /mcp-servers/{mcpServerId}/comments/{commentId} | Delete a MCP Server Comment
[**editCommentOfAPI**](CommentsApi.md#editCommentOfAPI) | **PATCH** /apis/{apiId}/comments/{commentId} | Edit a comment
[**editCommentOfMCPServer**](CommentsApi.md#editCommentOfMCPServer) | **PATCH** /mcp-servers/{mcpServerId}/comments/{commentId} | Edit a comment
[**getAllCommentsOfAPI**](CommentsApi.md#getAllCommentsOfAPI) | **GET** /apis/{apiId}/comments | Retrieve API Comments
[**getAllCommentsOfMCPServer**](CommentsApi.md#getAllCommentsOfMCPServer) | **GET** /mcp-servers/{mcpServerId}/comments | Retrieve MCP Server Comments
[**getCommentOfAPI**](CommentsApi.md#getCommentOfAPI) | **GET** /apis/{apiId}/comments/{commentId} | Get Details of an API Comment
[**getCommentOfMCPServer**](CommentsApi.md#getCommentOfMCPServer) | **GET** /mcp-servers/{mcpServerId}/comments/{commentId} | Get Details of a MCP Server Comment
[**getRepliesOfComment**](CommentsApi.md#getRepliesOfComment) | **GET** /apis/{apiId}/comments/{commentId}/replies | Get replies of a comment
[**getRepliesOfCommentOfMCPServer**](CommentsApi.md#getRepliesOfCommentOfMCPServer) | **GET** /mcp-servers/{mcpServerId}/comments/{commentId}/replies | Get replies of a comment


<a name="addCommentToAPI"></a>
# **addCommentToAPI**
> CommentDTO addCommentToAPI(apiId, postRequestBodyDTO, replyTo)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    PostRequestBodyDTO postRequestBodyDTO = new PostRequestBodyDTO(); // PostRequestBodyDTO | 
    String replyTo = "replyTo_example"; // String | ID of the perent comment. 
    try {
      CommentDTO result = apiInstance.addCommentToAPI(apiId, postRequestBodyDTO, replyTo);
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
 **postRequestBodyDTO** | [**PostRequestBodyDTO**](PostRequestBodyDTO.md)|  |
 **replyTo** | **String**| ID of the perent comment.  | [optional]

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
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

<a name="addCommentToMCPServer"></a>
# **addCommentToMCPServer**
> CommentDTO addCommentToMCPServer(mcpServerId, postRequestBodyDTO, replyTo)

Add a MCP Server Comment

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    PostRequestBodyDTO postRequestBodyDTO = new PostRequestBodyDTO(); // PostRequestBodyDTO | Comment object that should to be added 
    String replyTo = "replyTo_example"; // String | ID of the perent comment. 
    try {
      CommentDTO result = apiInstance.addCommentToMCPServer(mcpServerId, postRequestBodyDTO, replyTo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#addCommentToMCPServer");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **postRequestBodyDTO** | [**PostRequestBodyDTO**](PostRequestBodyDTO.md)| Comment object that should to be added  |
 **replyTo** | **String**| ID of the perent comment.  | [optional]

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
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
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
**403** | Forbidden. The request must be conditional but no condition has been specified.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**405** | MethodNotAllowed. Request method is known by the server but is not supported by the target resource.  |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteCommentOfMCPServer"></a>
# **deleteCommentOfMCPServer**
> deleteCommentOfMCPServer(mcpServerId, commentId, ifMatch)

Delete a MCP Server Comment

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String commentId = "commentId_example"; // String | Comment Id 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteCommentOfMCPServer(mcpServerId, commentId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#deleteCommentOfMCPServer");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **commentId** | **String**| Comment Id  |
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
**403** | Forbidden. The request must be conditional but no condition has been specified.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**405** | MethodNotAllowed. Request method is known by the server but is not supported by the target resource.  |  -  |
**500** | Internal Server Error. |  -  |

<a name="editCommentOfAPI"></a>
# **editCommentOfAPI**
> CommentDTO editCommentOfAPI(commentId, apiId, patchRequestBodyDTO)

Edit a comment

Edit the individual comment 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String commentId = "commentId_example"; // String | Comment Id 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    PatchRequestBodyDTO patchRequestBodyDTO = new PatchRequestBodyDTO(); // PatchRequestBodyDTO | 
    try {
      CommentDTO result = apiInstance.editCommentOfAPI(commentId, apiId, patchRequestBodyDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#editCommentOfAPI");
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
 **patchRequestBodyDTO** | [**PatchRequestBodyDTO**](PatchRequestBodyDTO.md)|  |

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
**200** | OK. Comment updated.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  * Location - Location to the newly created Comment.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

<a name="editCommentOfMCPServer"></a>
# **editCommentOfMCPServer**
> CommentDTO editCommentOfMCPServer(mcpServerId, commentId, patchRequestBodyDTO)

Edit a comment

Edit the individual comment 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String commentId = "commentId_example"; // String | Comment Id 
    PatchRequestBodyDTO patchRequestBodyDTO = new PatchRequestBodyDTO(); // PatchRequestBodyDTO | Comment object that should to be updated 
    try {
      CommentDTO result = apiInstance.editCommentOfMCPServer(mcpServerId, commentId, patchRequestBodyDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#editCommentOfMCPServer");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **commentId** | **String**| Comment Id  |
 **patchRequestBodyDTO** | [**PatchRequestBodyDTO**](PatchRequestBodyDTO.md)| Comment object that should to be updated  |

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
**200** | OK. Comment updated.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional request.  <br>  * Location - Location to the newly created Comment.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
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
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAllCommentsOfMCPServer"></a>
# **getAllCommentsOfMCPServer**
> CommentListDTO getAllCommentsOfMCPServer(mcpServerId, xWSO2Tenant, limit, offset, includeCommenterInfo)

Retrieve MCP Server Comments

Get a list of Comments that are already added to MCP Servers 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    try {
      CommentListDTO result = apiInstance.getAllCommentsOfMCPServer(mcpServerId, xWSO2Tenant, limit, offset, includeCommenterInfo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#getAllCommentsOfMCPServer");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
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
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getCommentOfAPI"></a>
# **getCommentOfAPI**
> CommentDTO getCommentOfAPI(commentId, apiId, xWSO2Tenant, ifNoneMatch, includeCommenterInfo, replyLimit, replyOffset)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String commentId = "commentId_example"; // String | Comment Id 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    Integer replyLimit = 25; // Integer | Maximum size of replies array to return. 
    Integer replyOffset = 0; // Integer | Starting point within the complete list of replies. 
    try {
      CommentDTO result = apiInstance.getCommentOfAPI(commentId, apiId, xWSO2Tenant, ifNoneMatch, includeCommenterInfo, replyLimit, replyOffset);
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
 **replyLimit** | **Integer**| Maximum size of replies array to return.  | [optional] [default to 25]
 **replyOffset** | **Integer**| Starting point within the complete list of replies.  | [optional] [default to 0]

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
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getCommentOfMCPServer"></a>
# **getCommentOfMCPServer**
> CommentDTO getCommentOfMCPServer(mcpServerId, commentId, xWSO2Tenant, ifNoneMatch, includeCommenterInfo, replyLimit, replyOffset)

Get Details of a MCP Server Comment

Get the individual comment given by a user for a certain MCP Server. 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String commentId = "commentId_example"; // String | Comment Id 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    Integer replyLimit = 25; // Integer | Maximum size of replies array to return. 
    Integer replyOffset = 0; // Integer | Starting point within the complete list of replies. 
    try {
      CommentDTO result = apiInstance.getCommentOfMCPServer(mcpServerId, commentId, xWSO2Tenant, ifNoneMatch, includeCommenterInfo, replyLimit, replyOffset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#getCommentOfMCPServer");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **commentId** | **String**| Comment Id  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
 **includeCommenterInfo** | **Boolean**| Whether we need to display commentor details.  | [optional] [default to false]
 **replyLimit** | **Integer**| Maximum size of replies array to return.  | [optional] [default to 25]
 **replyOffset** | **Integer**| Starting point within the complete list of replies.  | [optional] [default to 0]

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
**200** | OK. Comment returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getRepliesOfComment"></a>
# **getRepliesOfComment**
> CommentListDTO getRepliesOfComment(commentId, apiId, xWSO2Tenant, limit, offset, ifNoneMatch, includeCommenterInfo)

Get replies of a comment

Get replies of a comment 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String commentId = "commentId_example"; // String | Comment Id 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    try {
      CommentListDTO result = apiInstance.getRepliesOfComment(commentId, apiId, xWSO2Tenant, limit, offset, ifNoneMatch, includeCommenterInfo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#getRepliesOfComment");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
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
**200** | OK. Comment returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests.  <br>  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getRepliesOfCommentOfMCPServer"></a>
# **getRepliesOfCommentOfMCPServer**
> CommentListDTO getRepliesOfCommentOfMCPServer(mcpServerId, commentId, xWSO2Tenant, limit, offset, ifNoneMatch, includeCommenterInfo)

Get replies of a comment

Get replies of a comment 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    CommentsApi apiInstance = new CommentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String commentId = "commentId_example"; // String | Comment Id 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    Boolean includeCommenterInfo = false; // Boolean | Whether we need to display commentor details. 
    try {
      CommentListDTO result = apiInstance.getRepliesOfCommentOfMCPServer(mcpServerId, commentId, xWSO2Tenant, limit, offset, ifNoneMatch, includeCommenterInfo);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling CommentsApi#getRepliesOfCommentOfMCPServer");
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
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server.  |
 **commentId** | **String**| Comment Id  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
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
**200** | OK. Comment returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

