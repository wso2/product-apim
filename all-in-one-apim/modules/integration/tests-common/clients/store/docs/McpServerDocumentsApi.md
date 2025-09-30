# McpServerDocumentsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getMCPServerDocument**](McpServerDocumentsApi.md#getMCPServerDocument) | **GET** /mcp-servers/{mcpServerId}/documents/{documentId} | Get a Document of a MCP Server 
[**getMCPServerDocumentContent**](McpServerDocumentsApi.md#getMCPServerDocumentContent) | **GET** /mcp-servers/{mcpServerId}/documents/{documentId}/content | Get the Content of a MCP Server Document 
[**getMCPServerDocuments**](McpServerDocumentsApi.md#getMCPServerDocuments) | **GET** /mcp-servers/{mcpServerId}/documents | Get a List of Documents of a MCP Server 


<a name="getMCPServerDocument"></a>
# **getMCPServerDocument**
> DocumentDTO getMCPServerDocument(mcpServerId, documentId, xWSO2Tenant, ifNoneMatch)

Get a Document of a MCP Server 

This operation can be used to retrieve a particular document&#39;s metadata associated with a MCP Server.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve a document of a MCP Server that belongs to a different tenant  domain. If not specified super tenant will be used. If Authorization header is present in the request, the  user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted MCP  Server&#39;s document, you need to provide Authorization header. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      DocumentDTO result = apiInstance.getMCPServerDocument(mcpServerId, documentId, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#getMCPServerDocument");
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
 **documentId** | **String**| Document Identifier  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Document returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerDocumentContent"></a>
# **getMCPServerDocumentContent**
> getMCPServerDocumentContent(mcpServerId, documentId, xWSO2Tenant, ifNoneMatch)

Get the Content of a MCP Server Document 

This operation can be used to retrieve the content of a MCP Server&#39;s document.  The document can be of 3 types. In each cases responses are different.  1. **Inline type**:    The content of the document will be retrieved in &#x60;text/plain&#x60; content type 2. **FILE type**:    The file will be downloaded with the related content type (eg. &#x60;application/pdf&#x60;) 3. **URL type**:     The client will recieve the URL of the document as the Location header with the response with - &#x60;303 See Other&#x60;  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve the content of a document of a MCP Server that belongs to a  different tenant domain. If not specified super tenant will be used. If Authorization header is present in the  request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted MCP  Server&#39;s document content, you need to provide Authorization header. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      apiInstance.getMCPServerDocumentContent(mcpServerId, documentId, xWSO2Tenant, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#getMCPServerDocumentContent");
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
 **documentId** | **String**| Document Identifier  |
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
**200** | OK. File or inline content returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests.  <br>  |
**303** | See Other. Source can be retrieved from the URL specified at the Location header.  |  * Location - The Source URL of the document.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerDocuments"></a>
# **getMCPServerDocuments**
> DocumentListDTO getMCPServerDocuments(mcpServerId, limit, offset, xWSO2Tenant, ifNoneMatch)

Get a List of Documents of a MCP Server 

This operation can be used to retrieve a list of documents belonging to a MCP Server by providing the id of the  MCP Server.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve documents of a MCP Server that belongs to a different tenant  domain. If not specified super tenant will be used. If Authorization header is present in the request, the  user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted MCP  Server&#39;s documents, you need to provide Authorization header. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      DocumentListDTO result = apiInstance.getMCPServerDocuments(mcpServerId, limit, offset, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#getMCPServerDocuments");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**DocumentListDTO**](DocumentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Document list is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

