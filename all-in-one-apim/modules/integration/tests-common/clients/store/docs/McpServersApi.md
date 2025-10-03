# McpServersApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllMCPServers**](McpServersApi.md#getAllMCPServers) | **GET** /mcp-servers | Retrieve/Search MCP Servers 
[**getMCPServer**](McpServersApi.md#getMCPServer) | **GET** /mcp-servers/{mcpServerId} | Get Details of a MCP Server 
[**getMCPServerSubscriptionPolicies**](McpServersApi.md#getMCPServerSubscriptionPolicies) | **GET** /mcp-servers/{mcpServerId}/subscription-policies | Get Details of the Subscription Throttling Policies of a MCP Server 
[**getMCPServerThumbnail**](McpServersApi.md#getMCPServerThumbnail) | **GET** /mcp-servers/{mcpServerId}/thumbnail | Get Thumbnail Image


<a name="getAllMCPServers"></a>
# **getAllMCPServers**
> APIListDTO getAllMCPServers(limit, offset, xWSO2Tenant, query, ifNoneMatch)

Retrieve/Search MCP Servers 

This operation provides you a list of available MCP servers qualifying under a given search condition.  Each retrieved MCP Server is represented with a minimal amount of attributes. If you want to get complete  details of a MCP server, you need to use **Get details of a MCP** operation.  This operation supports retrieving MCP servers of other tenants. The required tenant domain need to be  specified as a header &#x60;X-WSO2-Tenant&#x60;. If not specified super tenant&#39;s MCP servers will be retrieved. If you  used an Authorization header, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * By default, this operation retrieves Published MCP Server.  * This operation does not require an Authorization header by default. But if it is provided, it will be validated and checked for permissions of the user, hence you may be able to see APIs which are restricted for special permissions/roles. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an MCP Server if the provider of the MCP Server is exactly \"wso2\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match a MCP Server if the provider of the MCP Server starts with \"wso2\".  Supported attribute modifiers are [**version, context, status, description, doc, provider, tag**]  To search by Properties provide the query in below format.  **property_name:property_value**  Eg. \"environment:test\" where environment is the property name and test is the propert value.  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      APIListDTO result = apiInstance.getAllMCPServers(limit, offset, xWSO2Tenant, query, ifNoneMatch);
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an MCP Server if the provider of the MCP Server is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match a MCP Server if the provider of the MCP Server starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, status, description, doc, provider, tag**]  To search by Properties provide the query in below format.  **property_name:property_value**  Eg. \&quot;environment:test\&quot; where environment is the property name and test is the propert value.  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

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
**200** | OK. List of qualifying MCP Servers is returned.  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServer"></a>
# **getMCPServer**
> APIDTO getMCPServer(mcpServerId, xWSO2Tenant, ifNoneMatch)

Get Details of a MCP Server 

Using this operation, you can retrieve complete details of a single MCP Server. You need to provide the Id of  the MCP Server to retrieve it.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve a MCP Server of a different tenant domain. If not specified  super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated  with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But if it is provided, it will be  validated and checked for permissions of the user, hence you may be able to see APIs which are restricted for special permissions/roles. \\n 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      APIDTO result = apiInstance.getMCPServer(mcpServerId, xWSO2Tenant, ifNoneMatch);
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerSubscriptionPolicies"></a>
# **getMCPServerSubscriptionPolicies**
> ThrottlingPolicyDTO getMCPServerSubscriptionPolicies(mcpServerId, xWSO2Tenant, ifNoneMatch)

Get Details of the Subscription Throttling Policies of a MCP Server 

This operation can be used to retrieve details of the subscription throttling policy of a MCP Server by  specifying the MCP Server Id.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve MCP Server subscription throttling policies that belongs to a  different tenant domain. If not specified super tenant will be used. If Authorization header is present in the  request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      ThrottlingPolicyDTO result = apiInstance.getMCPServerSubscriptionPolicies(mcpServerId, xWSO2Tenant, ifNoneMatch);
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

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
**200** | OK. Throttling Policy returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerThumbnail"></a>
# **getMCPServerThumbnail**
> getMCPServerThumbnail(mcpServerId, xWSO2Tenant, ifNoneMatch)

Get Thumbnail Image

This operation can be used to download a thumbnail image of a MCP Server.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve a thumbnail of a MCP Server that belongs to a different tenant  domain. If not specified super tenant will be used. If Authorization header is present in the request, the  user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted  MCP Server&#39;s thumbnail, you need to provide Authorization header. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServersApi apiInstance = new McpServersApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      apiInstance.getMCPServerThumbnail(mcpServerId, xWSO2Tenant, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServersApi#getMCPServerThumbnail");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

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
**200** | OK. Thumbnail image returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

