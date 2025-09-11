# McpServerBackendsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getMCPServerBackend**](McpServerBackendsApi.md#getMCPServerBackend) | **GET** /mcp-servers/{mcpServerId}/backends/{backendId} | Get backends of a MCP Server
[**getMCPServerBackends**](McpServerBackendsApi.md#getMCPServerBackends) | **GET** /mcp-servers/{mcpServerId}/backends | Get a list of backends of a MCP Server
[**updateMCPServerBackend**](McpServerBackendsApi.md#updateMCPServerBackend) | **PUT** /mcp-servers/{mcpServerId}/backends/{backendId} | Update a backend of a MCP Server


<a name="getMCPServerBackend"></a>
# **getMCPServerBackend**
> BackendDTO getMCPServerBackend(mcpServerId, backendId)

Get backends of a MCP Server

This operation can be used to get a backend of a MCP Server 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerBackendsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerBackendsApi apiInstance = new McpServerBackendsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String backendId = "backendId_example"; // String | **Backend ID** consisting of the **UUID** of the Backend**. 
    try {
      BackendDTO result = apiInstance.getMCPServerBackend(mcpServerId, backendId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerBackendsApi#getMCPServerBackend");
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
 **backendId** | **String**| **Backend ID** consisting of the **UUID** of the Backend**.  |

### Return type

[**BackendDTO**](BackendDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Backend object is returned.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getMCPServerBackends"></a>
# **getMCPServerBackends**
> BackendListDTO getMCPServerBackends(mcpServerId)

Get a list of backends of a MCP Server

This operation can be used to get a list of backends of a MCP server by the MCP Server UUID. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerBackendsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerBackendsApi apiInstance = new McpServerBackendsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    try {
      BackendListDTO result = apiInstance.getMCPServerBackends(mcpServerId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerBackendsApi#getMCPServerBackends");
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

[**BackendListDTO**](BackendListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. A list of Backend objects are returned.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="updateMCPServerBackend"></a>
# **updateMCPServerBackend**
> BackendDTO updateMCPServerBackend(mcpServerId, backendId, backendDTO)

Update a backend of a MCP Server

This operation can be used to update a backend of a MCP Server 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerBackendsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerBackendsApi apiInstance = new McpServerBackendsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String backendId = "backendId_example"; // String | **Backend ID** consisting of the **UUID** of the Backend**. 
    BackendDTO backendDTO = new BackendDTO(); // BackendDTO | Backend object with updated details
    try {
      BackendDTO result = apiInstance.updateMCPServerBackend(mcpServerId, backendId, backendDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerBackendsApi#updateMCPServerBackend");
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
 **backendId** | **String**| **Backend ID** consisting of the **UUID** of the Backend**.  |
 **backendDTO** | [**BackendDTO**](BackendDTO.md)| Backend object with updated details | [optional]

### Return type

[**BackendDTO**](BackendDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Updated Backend is returned.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

