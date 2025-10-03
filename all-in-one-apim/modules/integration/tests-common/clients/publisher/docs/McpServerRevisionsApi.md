# McpServerRevisionsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createMCPServerRevision**](McpServerRevisionsApi.md#createMCPServerRevision) | **POST** /mcp-servers/{mcpServerId}/revisions | Create MCP Server Revision
[**deleteMCPServerRevision**](McpServerRevisionsApi.md#deleteMCPServerRevision) | **DELETE** /mcp-servers/{mcpServerId}/revisions/{revisionId} | Delete a MCP Server Revision
[**deleteMCPServerRevisionDeploymentPendingTask**](McpServerRevisionsApi.md#deleteMCPServerRevisionDeploymentPendingTask) | **DELETE** /mcp-servers/{mcpServerId}/cancel-revision-workflow/{revisionId}/{envName} | Delete Pending Revision Deployment Workflow Tasks
[**deployMCPServerRevision**](McpServerRevisionsApi.md#deployMCPServerRevision) | **POST** /mcp-servers/{mcpServerId}/deploy-revision | Deploy Revision
[**getMCPServerRevision**](McpServerRevisionsApi.md#getMCPServerRevision) | **GET** /mcp-servers/{mcpServerId}/revisions/{revisionId} | Retrieve revision of a MCP Server.
[**getMCPServerRevisionDeployments**](McpServerRevisionsApi.md#getMCPServerRevisionDeployments) | **GET** /mcp-servers/{mcpServerId}/deployments | List Deployments
[**getMCPServerRevisions**](McpServerRevisionsApi.md#getMCPServerRevisions) | **GET** /mcp-servers/{mcpServerId}/revisions | List Revisions
[**restoreMCPServerRevision**](McpServerRevisionsApi.md#restoreMCPServerRevision) | **POST** /mcp-servers/{mcpServerId}/restore-revision | Restore a MCP Server Revision
[**undeployMCPServerRevision**](McpServerRevisionsApi.md#undeployMCPServerRevision) | **POST** /mcp-servers/{mcpServerId}/undeploy-revision | UnDeploy Revision of a MCP Server
[**updateMCPServerDeployment**](McpServerRevisionsApi.md#updateMCPServerDeployment) | **PUT** /mcp-servers/{mcpServerId}/deployments/{deploymentId} | Update Deployment


<a name="createMCPServerRevision"></a>
# **createMCPServerRevision**
> APIRevisionDTO createMCPServerRevision(mcpServerId, apIRevisionDTO)

Create MCP Server Revision

Create a new MCP Server revision 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    APIRevisionDTO apIRevisionDTO = new APIRevisionDTO(); // APIRevisionDTO | API object that needs to be added
    try {
      APIRevisionDTO result = apiInstance.createMCPServerRevision(mcpServerId, apIRevisionDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#createMCPServerRevision");
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
 **apIRevisionDTO** | [**APIRevisionDTO**](APIRevisionDTO.md)| API object that needs to be added | [optional]

### Return type

[**APIRevisionDTO**](APIRevisionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created APIRevision object as the entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="deleteMCPServerRevision"></a>
# **deleteMCPServerRevision**
> APIRevisionListDTO deleteMCPServerRevision(mcpServerId, revisionId)

Delete a MCP Server Revision

Delete a revision of a MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIRevisionListDTO result = apiInstance.deleteMCPServerRevision(mcpServerId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#deleteMCPServerRevision");
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
 **revisionId** | **String**| Revision ID of an API  |

### Return type

[**APIRevisionListDTO**](APIRevisionListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of remaining MCP servers revisions are returned.  |  -  |
**204** | No Content. Successfully deleted the revision  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="deleteMCPServerRevisionDeploymentPendingTask"></a>
# **deleteMCPServerRevisionDeploymentPendingTask**
> deleteMCPServerRevisionDeploymentPendingTask(mcpServerId, revisionId, envName)

Delete Pending Revision Deployment Workflow Tasks

This operation can be used to remove pending revision deployment requests that are in pending state 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    String envName = "envName_example"; // String | Environment name of an Revision 
    try {
      apiInstance.deleteMCPServerRevisionDeploymentPendingTask(mcpServerId, revisionId, envName);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#deleteMCPServerRevisionDeploymentPendingTask");
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
 **revisionId** | **String**| Revision ID of an API  |
 **envName** | **String**| Environment name of an Revision  |

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
**200** | OK. Revision deployment pending task removed successfully.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="deployMCPServerRevision"></a>
# **deployMCPServerRevision**
> List&lt;APIRevisionDeploymentDTO&gt; deployMCPServerRevision(mcpServerId, revisionId, apIRevisionDeploymentDTO)

Deploy Revision

Deploy a revision of a MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      List<APIRevisionDeploymentDTO> result = apiInstance.deployMCPServerRevision(mcpServerId, revisionId, apIRevisionDeploymentDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#deployMCPServerRevision");
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
 **revisionId** | **String**| Revision ID of an API  | [optional]
 **apIRevisionDeploymentDTO** | [**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be added | [optional]

### Return type

[**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Created. Successful response with the newly deployed APIRevisionDeployment List object as the entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getMCPServerRevision"></a>
# **getMCPServerRevision**
> APIRevisionDTO getMCPServerRevision(mcpServerId, revisionId)

Retrieve revision of a MCP Server.

Retrieve a revision of a MCP server 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIRevisionDTO result = apiInstance.getMCPServerRevision(mcpServerId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#getMCPServerRevision");
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
 **revisionId** | **String**| Revision ID of an API  |

### Return type

[**APIRevisionDTO**](APIRevisionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. A MCP server revision is returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getMCPServerRevisionDeployments"></a>
# **getMCPServerRevisionDeployments**
> APIRevisionDeploymentListDTO getMCPServerRevisionDeployments(mcpServerId)

List Deployments

List available deployed revision deployment details of a MCP Server 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    try {
      APIRevisionDeploymentListDTO result = apiInstance.getMCPServerRevisionDeployments(mcpServerId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#getMCPServerRevisionDeployments");
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

### Return type

[**APIRevisionDeploymentListDTO**](APIRevisionDeploymentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of deployed revision deployment details are returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getMCPServerRevisions"></a>
# **getMCPServerRevisions**
> APIRevisionListDTO getMCPServerRevisions(mcpServerId, query)

List Revisions

List available revisions of a MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String query = "query_example"; // String | 
    try {
      APIRevisionListDTO result = apiInstance.getMCPServerRevisions(mcpServerId, query);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#getMCPServerRevisions");
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
 **query** | **String**|  | [optional]

### Return type

[**APIRevisionListDTO**](APIRevisionListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of MCP server revisions are returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="restoreMCPServerRevision"></a>
# **restoreMCPServerRevision**
> MCPServerDTO restoreMCPServerRevision(mcpServerId, revisionId)

Restore a MCP Server Revision

Restore a revision to the current MCP server 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      MCPServerDTO result = apiInstance.restoreMCPServerRevision(mcpServerId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#restoreMCPServerRevision");
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
 **revisionId** | **String**| Revision ID of an API  | [optional]

### Return type

[**MCPServerDTO**](MCPServerDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Restored. Successful response with the newly restored API object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="undeployMCPServerRevision"></a>
# **undeployMCPServerRevision**
> undeployMCPServerRevision(mcpServerId, revisionId, revisionNumber, allEnvironments, apIRevisionDeploymentDTO)

UnDeploy Revision of a MCP Server

UnDeploy a revision of a MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    String revisionNumber = "revisionNumber_example"; // String | Revision Number of an API 
    Boolean allEnvironments = false; // Boolean | 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      apiInstance.undeployMCPServerRevision(mcpServerId, revisionId, revisionNumber, allEnvironments, apIRevisionDeploymentDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#undeployMCPServerRevision");
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
 **revisionId** | **String**| Revision ID of an API  | [optional]
 **revisionNumber** | **String**| Revision Number of an API  | [optional]
 **allEnvironments** | **Boolean**|  | [optional] [default to false]
 **apIRevisionDeploymentDTO** | [**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be added | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK.  |  -  |
**201** | Created. Successful response with the newly undeployed APIRevisionDeploymentList object as the entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="updateMCPServerDeployment"></a>
# **updateMCPServerDeployment**
> APIRevisionDeploymentDTO updateMCPServerDeployment(mcpServerId, deploymentId, apIRevisionDeploymentDTO)

Update Deployment

Update deployment devportal visibility 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerRevisionsApi apiInstance = new McpServerRevisionsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String deploymentId = "deploymentId_example"; // String | Base64 URL encoded value of the name of an environment 
    APIRevisionDeploymentDTO apIRevisionDeploymentDTO = new APIRevisionDeploymentDTO(); // APIRevisionDeploymentDTO | Deployment object that needs to be updated
    try {
      APIRevisionDeploymentDTO result = apiInstance.updateMCPServerDeployment(mcpServerId, deploymentId, apIRevisionDeploymentDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerRevisionsApi#updateMCPServerDeployment");
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
 **deploymentId** | **String**| Base64 URL encoded value of the name of an environment  |
 **apIRevisionDeploymentDTO** | [**APIRevisionDeploymentDTO**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be updated | [optional]

### Return type

[**APIRevisionDeploymentDTO**](APIRevisionDeploymentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Created. Successful response with the newly updated APIRevisionDeployment List object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

