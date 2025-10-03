# McpServersApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createMCPServerFromAPI**](McpServersApi.md#createMCPServerFromAPI) | **POST** /mcp-servers/generate-from-api | Create a New MCP Server
[**createMCPServerFromOpenAPI**](McpServersApi.md#createMCPServerFromOpenAPI) | **POST** /mcp-servers/generate-from-openapi | Create a MCP server using an OpenAPI definition. 
[**createMCPServerProxy**](McpServersApi.md#createMCPServerProxy) | **POST** /mcp-servers/generate-from-mcp-server | Create an MCP server by proxying a third-party MCP Server 
[**createNewMCPServerVersion**](McpServersApi.md#createNewMCPServerVersion) | **POST** /mcp-servers/copy-mcp-server | Create a New MCP Server Version
[**deleteMCPServer**](McpServersApi.md#deleteMCPServer) | **DELETE** /mcp-servers/{mcpServerId} | Delete a MCP Server
[**generateInternalAPIKeyMCPServer**](McpServersApi.md#generateInternalAPIKeyMCPServer) | **POST** /mcp-servers/{mcpServerId}/generate-key | Generate internal API Key to invoke MCP Server.
[**getAllMCPServers**](McpServersApi.md#getAllMCPServers) | **GET** /mcp-servers | Retrieve/Search MCP Servers 
[**getMCPServer**](McpServersApi.md#getMCPServer) | **GET** /mcp-servers/{mcpServerId} | Get Details of an MCP Server
[**getMCPServerSubscriptionPolicies**](McpServersApi.md#getMCPServerSubscriptionPolicies) | **GET** /mcp-servers/{mcpServerId}/subscription-policies | Get Details of the Subscription Throttling Policies of a MCP Server 
[**updateMCPServer**](McpServersApi.md#updateMCPServer) | **PUT** /mcp-servers/{mcpServerId} | Update a MCP Server


<a name="createMCPServerFromAPI"></a>
# **createMCPServerFromAPI**
> MCPServerDTO createMCPServerFromAPI(mcPServerDTO, openAPIVersion)

Create a New MCP Server

This operation can be used to create a new MCP server using an existing API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    MCPServerDTO mcPServerDTO = new MCPServerDTO(); // MCPServerDTO | API object that needs to be added
    String openAPIVersion = "v3"; // String | Open API version
    try {
      MCPServerDTO result = apiInstance.createMCPServerFromAPI(mcPServerDTO, openAPIVersion);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#createMCPServerFromAPI");
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
 **mcPServerDTO** | [**MCPServerDTO**](MCPServerDTO.md)| API object that needs to be added |
 **openAPIVersion** | **String**| Open API version | [optional] [default to v3] [enum: v2, v3]

### Return type

[**MCPServerDTO**](MCPServerDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="createMCPServerFromOpenAPI"></a>
# **createMCPServerFromOpenAPI**
> MCPServerDTO createMCPServerFromOpenAPI(file, url, additionalProperties)

Create a MCP server using an OpenAPI definition. 

This operation can be used to create a MCP server using the OpenAPI definition. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition.  Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    File file = new File("/path/to/file"); // File | Definition to upload as a file
    String url = "url_example"; // String | Definition url
    String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with MCP Server's schema
    try {
      MCPServerDTO result = apiInstance.createMCPServerFromOpenAPI(file, url, additionalProperties);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#createMCPServerFromOpenAPI");
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
 **file** | **File**| Definition to upload as a file | [optional]
 **url** | **String**| Definition url | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with MCP Server&#39;s schema | [optional]

### Return type

[**MCPServerDTO**](MCPServerDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="createMCPServerProxy"></a>
# **createMCPServerProxy**
> MCPServerDTO createMCPServerProxy(mcPServerProxyRequestDTO)

Create an MCP server by proxying a third-party MCP Server 

This operation can be used to create a MCP server using a third party MCP Server.  Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    MCPServerProxyRequestDTO mcPServerProxyRequestDTO = new MCPServerProxyRequestDTO(); // MCPServerProxyRequestDTO | 
    try {
      MCPServerDTO result = apiInstance.createMCPServerProxy(mcPServerProxyRequestDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#createMCPServerProxy");
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
 **mcPServerProxyRequestDTO** | [**MCPServerProxyRequestDTO**](MCPServerProxyRequestDTO.md)|  | [optional]

### Return type

[**MCPServerDTO**](MCPServerDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="createNewMCPServerVersion"></a>
# **createNewMCPServerVersion**
> MCPServerDTO createNewMCPServerVersion(newVersion, mcpServerId, defaultVersion, serviceVersion)

Create a New MCP Server Version

This operation can be used to create a new version of an existing MCP server. The new version is specified as  &#x60;newVersion&#x60; query parameter. New MCP server will be in &#x60;CREATED&#x60; state. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String newVersion = "newVersion_example"; // String | Version of the new MCP server.
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server**. 
    Boolean defaultVersion = false; // Boolean | Specifies whether new MCP server should be added as default version.
    String serviceVersion = "serviceVersion_example"; // String | Version of the Service that will used in creating new version
    try {
      MCPServerDTO result = apiInstance.createNewMCPServerVersion(newVersion, mcpServerId, defaultVersion, serviceVersion);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#createNewMCPServerVersion");
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
 **newVersion** | **String**| Version of the new MCP server. |
 **mcpServerId** | **String**| **MCP Server ID** consisting of the **UUID** of the MCP Server**.  |
 **defaultVersion** | **Boolean**| Specifies whether new MCP server should be added as default version. | [optional] [default to false]
 **serviceVersion** | **String**| Version of the Service that will used in creating new version | [optional]

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
**201** | Created. Successful response with the newly created MCP server as entity in the body. Location header contains URL of newly created MCP server.  |  * Location - The URL of the newly created API.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="deleteMCPServer"></a>
# **deleteMCPServer**
> deleteMCPServer(mcpServerId, ifMatch)

Delete a MCP Server

This operation can be used to delete a MCP server by providing the Id of the MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteMCPServer(mcpServerId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#deleteMCPServer");
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
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="generateInternalAPIKeyMCPServer"></a>
# **generateInternalAPIKeyMCPServer**
> APIKeyDTO generateInternalAPIKeyMCPServer(mcpServerId)

Generate internal API Key to invoke MCP Server.

This operation can be used to generate internal api key which used to invoke MCP Server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    try {
      APIKeyDTO result = apiInstance.generateInternalAPIKeyMCPServer(mcpServerId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#generateInternalAPIKeyMCPServer");
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

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. apikey generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAllMCPServers"></a>
# **getAllMCPServers**
> APIListDTO getAllMCPServers(limit, offset, xWSO2Tenant, query, ifNoneMatch, accept)

Retrieve/Search MCP Servers 

This operation provides you a list of available MCP servers qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details,  you need to use **Get details of an MCP server** operation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
    String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an MCP server if the provider of the API contains \"wso2\". \"provider:\"wso2\"\" will match an API if the provider of the API is exactly \"wso2\". \"status:PUBLISHED\" will match an API if the API is in PUBLISHED state.  Also you can use combined modifiers Eg. name:pizzashack version:v1 will match an API if the name of the API is pizzashack and version is v1.  Supported attribute modifiers are [**version, context, name, status, description, provider, api-category, tags, doc, contexttemplate, lcstate, content, type, label, enablestore, thirdparty**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not  support URL encoding (such as curl) 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    try {
      APIListDTO result = apiInstance.getAllMCPServers(limit, offset, xWSO2Tenant, query, ifNoneMatch, accept);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#getAllMCPServers");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an MCP server if the provider of the API contains \&quot;wso2\&quot;. \&quot;provider:\&quot;wso2\&quot;\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;. \&quot;status:PUBLISHED\&quot; will match an API if the API is in PUBLISHED state.  Also you can use combined modifiers Eg. name:pizzashack version:v1 will match an API if the name of the API is pizzashack and version is v1.  Supported attribute modifiers are [**version, context, name, status, description, provider, api-category, tags, doc, contexttemplate, lcstate, content, type, label, enablestore, thirdparty**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not  support URL encoding (such as curl)  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]

### Return type

[**APIListDTO**](APIListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of qualifying APIs is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServer"></a>
# **getMCPServer**
> MCPServerDTO getMCPServer(mcpServerId, xWSO2Tenant, ifNoneMatch)

Get Details of an MCP Server

Using this operation, you can retrieve complete details of a single API. You need to provide the Id of the API  to retrieve it. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      MCPServerDTO result = apiInstance.getMCPServer(mcpServerId, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#getMCPServer");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

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
**200** | OK. Requested MCP Server is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerSubscriptionPolicies"></a>
# **getMCPServerSubscriptionPolicies**
> ThrottlingPolicyDTO getMCPServerSubscriptionPolicies(mcpServerId, xWSO2Tenant, ifNoneMatch, isAiApi, organizationID)

Get Details of the Subscription Throttling Policies of a MCP Server 

This operation can be used to retrieve details of the subscription throttling policy of a MCP server by  specifying the API Id.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive MCP server subscription throttling policies that belongs to a  different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    Boolean isAiApi = false; // Boolean | Indicates the quota policy type to be AI API quota or not. 
    String organizationID = "organizationID_example"; // String | Indicates the organization ID 
    try {
      ThrottlingPolicyDTO result = apiInstance.getMCPServerSubscriptionPolicies(mcpServerId, xWSO2Tenant, ifNoneMatch, isAiApi, organizationID);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#getMCPServerSubscriptionPolicies");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **isAiApi** | **Boolean**| Indicates the quota policy type to be AI API quota or not.  | [optional] [default to false]
 **organizationID** | **String**| Indicates the organization ID  | [optional]

### Return type

[**ThrottlingPolicyDTO**](ThrottlingPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Throttling Policy returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="updateMCPServer"></a>
# **updateMCPServer**
> MCPServerDTO updateMCPServer(mcpServerId, mcPServerDTO, ifMatch)

Update a MCP Server

This operation can be used to update an existing MCP Server. But the properties &#x60;name&#x60;, &#x60;version&#x60;, &#x60;context&#x60;, &#x60;provider&#x60;, &#x60;state&#x60; will not be changed by this operation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    MCPServerDTO mcPServerDTO = new MCPServerDTO(); // MCPServerDTO | API object that needs to be added
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      MCPServerDTO result = apiInstance.updateMCPServer(mcpServerId, mcPServerDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#updateMCPServer");
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
 **mcPServerDTO** | [**MCPServerDTO**](MCPServerDTO.md)| API object that needs to be added |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**MCPServerDTO**](MCPServerDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API object  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

