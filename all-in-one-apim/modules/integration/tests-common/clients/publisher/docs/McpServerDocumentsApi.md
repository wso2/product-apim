# McpServerDocumentsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addMCPServerDocument**](McpServerDocumentsApi.md#addMCPServerDocument) | **POST** /mcp-servers/{mcpServerId}/documents | Add a New Document to a MCP server
[**addMCPServerDocumentContent**](McpServerDocumentsApi.md#addMCPServerDocumentContent) | **POST** /mcp-servers/{mcpServerId}/documents/{documentId}/content | Upload the Content of a MCP Server Document
[**deleteMCPServerDocument**](McpServerDocumentsApi.md#deleteMCPServerDocument) | **DELETE** /mcp-servers/{mcpServerId}/documents/{documentId} | Delete a Document of a MCP Server
[**getMCPServerDocument**](McpServerDocumentsApi.md#getMCPServerDocument) | **GET** /mcp-servers/{mcpServerId}/documents/{documentId} | Get a Document of a MCP Server
[**getMCPServerDocumentContent**](McpServerDocumentsApi.md#getMCPServerDocumentContent) | **GET** /mcp-servers/{mcpServerId}/documents/{documentId}/content | Get the Content of a MCP Server Document
[**getMCPServerDocuments**](McpServerDocumentsApi.md#getMCPServerDocuments) | **GET** /mcp-servers/{mcpServerId}/documents | Get a List of Documents of a MCP Server
[**updateMCPServerDocument**](McpServerDocumentsApi.md#updateMCPServerDocument) | **PUT** /mcp-servers/{mcpServerId}/documents/{documentId} | Update a Document of a MCP Server
[**validateMCPServerDocument**](McpServerDocumentsApi.md#validateMCPServerDocument) | **POST** /mcp-servers/{mcpServerId}/documents/validate | Check Whether a Document with the Provided Name Exist


<a name="addMCPServerDocument"></a>
# **addMCPServerDocument**
> DocumentDTO addMCPServerDocument(mcpServerId, documentDTO)

Add a New Document to a MCP server

This operation can be used to add a new documentation to a MCP server. This operation only adds the metadata  of a document. To add the actual content we need to use **Upload the content of an MCP server document ** MCP server once we obtain a document Id by this operation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    DocumentDTO documentDTO = new DocumentDTO(); // DocumentDTO | Document object that needs to be added
    try {
      DocumentDTO result = apiInstance.addMCPServerDocument(mcpServerId, documentDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#addMCPServerDocument");
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
 **documentDTO** | [**DocumentDTO**](DocumentDTO.md)| Document object that needs to be added |

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created Document object as entity in the body. Location header contains URL of newly added document.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - Location to the newly created Document.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="addMCPServerDocumentContent"></a>
# **addMCPServerDocumentContent**
> DocumentDTO addMCPServerDocumentContent(mcpServerId, documentId, ifMatch, file, inlineContent)

Upload the Content of a MCP Server Document

This operation can be used to upload a file or add inline content to a MCP server document.  **IMPORTANT:** * Either **file** or **inlineContent** form data parameters should be specified at one time. * Document&#39;s source type should be **FILE** in order to upload a file to the document using **file** parameter. * Document&#39;s source type should be **INLINE** in order to add inline content to the document using **inlineContent** parameter. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    File file = new File("/path/to/file"); // File | Document to upload
    String inlineContent = "inlineContent_example"; // String | Inline content of the document
    try {
      DocumentDTO result = apiInstance.addMCPServerDocumentContent(mcpServerId, documentId, ifMatch, file, inlineContent);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#addMCPServerDocumentContent");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **file** | **File**| Document to upload | [optional]
 **inlineContent** | **String**| Inline content of the document | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Document updated  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the updated content of the document.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="deleteMCPServerDocument"></a>
# **deleteMCPServerDocument**
> deleteMCPServerDocument(mcpServerId, documentId, ifMatch)

Delete a Document of a MCP Server

This operation can be used to delete a document associated with a MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteMCPServerDocument(mcpServerId, documentId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#deleteMCPServerDocument");
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
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getMCPServerDocument"></a>
# **getMCPServerDocument**
> DocumentDTO getMCPServerDocument(mcpServerId, documentId, accept, ifNoneMatch)

Get a Document of a MCP Server

This operation can be used to retrieve a particular document&#39;s metadata associated with a MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      DocumentDTO result = apiInstance.getMCPServerDocument(mcpServerId, documentId, accept, ifNoneMatch);
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
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

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
**200** | OK. Document returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerDocumentContent"></a>
# **getMCPServerDocumentContent**
> getMCPServerDocumentContent(mcpServerId, documentId, accept, ifNoneMatch)

Get the Content of a MCP Server Document

This operation can be used to retrieve the content of a MCP server&#39;s document.  The document can be of 3 types. In each cases responses are different.  1. **Inline type**:    The content of the document will be retrieved in &#x60;text/plain&#x60; content type     _Sample cURL_ : &#x60;curl -k -H \&quot;Authorization:Bearer 579f0af4-37be-35c7-81a4-f1f1e9ee7c51\&quot; -F  inlineContent&#x3D;@\&quot;docs.txt\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v4/mcp-servers/995a4972-3178-4b17-a374-756e0e19127c/documents/43c2bcce-60e7-405f-bc36-e39c0c5e189e/content&#x60; 2. **FILE type**:    The file will be downloaded with the related content type (eg. &#x60;application/pdf&#x60;) 3. **URL type**:     The client will recieve the URL of the document as the Location header with the response with - &#x60;303 See Other&#x60; 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      apiInstance.getMCPServerDocumentContent(mcpServerId, documentId, accept, ifNoneMatch);
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
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

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
**200** | OK. File or inline content returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**303** | See Other. Source can be retrieved from the URL specified at the Location header.  |  * Location - The Source URL of the document.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getMCPServerDocuments"></a>
# **getMCPServerDocuments**
> DocumentListDTO getMCPServerDocuments(mcpServerId, limit, offset, accept, ifNoneMatch)

Get a List of Documents of a MCP Server

This operation can be used to retrieve a list of documents belonging to a MCP server by providing the ID of  the MCP server. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      DocumentListDTO result = apiInstance.getMCPServerDocuments(mcpServerId, limit, offset, accept, ifNoneMatch);
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
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

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
**200** | OK. Document list is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="updateMCPServerDocument"></a>
# **updateMCPServerDocument**
> DocumentDTO updateMCPServerDocument(mcpServerId, documentId, documentDTO, ifMatch)

Update a Document of a MCP Server

This operation can be used to update metadata of an MCP server&#39;s document. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String documentId = "documentId_example"; // String | Document Identifier 
    DocumentDTO documentDTO = new DocumentDTO(); // DocumentDTO | Document object that needs to be added
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      DocumentDTO result = apiInstance.updateMCPServerDocument(mcpServerId, documentId, documentDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#updateMCPServerDocument");
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
 **documentDTO** | [**DocumentDTO**](DocumentDTO.md)| Document object that needs to be added |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Document updated  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the updated document.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="validateMCPServerDocument"></a>
# **validateMCPServerDocument**
> validateMCPServerDocument(mcpServerId, name, ifMatch)

Check Whether a Document with the Provided Name Exist

This operation can be used to verify the document name exists or not. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.McpServerDocumentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    McpServerDocumentsApi apiInstance = new McpServerDocumentsApi(defaultClient);
    String mcpServerId = "mcpServerId_example"; // String | **MCP Server ID** consisting of the **UUID** of the MCP Server. 
    String name = "name_example"; // String | The name of the document which needs to be checked for the existence. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.validateMCPServerDocument(mcpServerId, name, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling McpServerDocumentsApi#validateMCPServerDocument");
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
 **name** | **String**| The name of the document which needs to be checked for the existence.  |
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
**200** | OK. Successful response if the document name exists.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist.  |  -  |

