# ApiProductDocumentsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiProductsApiProductIdDocumentsDocumentIdContentGet**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsDocumentIdContentGet) | **GET** /api-products/{apiProductId}/documents/{documentId}/content | Get the content of an API Product document
[**apiProductsApiProductIdDocumentsDocumentIdContentPost**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsDocumentIdContentPost) | **POST** /api-products/{apiProductId}/documents/{documentId}/content | Upload the content of an API Product document
[**apiProductsApiProductIdDocumentsDocumentIdDelete**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsDocumentIdDelete) | **DELETE** /api-products/{apiProductId}/documents/{documentId} | Delete a document of an API Product
[**apiProductsApiProductIdDocumentsDocumentIdGet**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsDocumentIdGet) | **GET** /api-products/{apiProductId}/documents/{documentId} | Get a document of an API
[**apiProductsApiProductIdDocumentsDocumentIdPut**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsDocumentIdPut) | **PUT** /api-products/{apiProductId}/documents/{documentId} | Update a document of an API Product
[**apiProductsApiProductIdDocumentsGet**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsGet) | **GET** /api-products/{apiProductId}/documents | Get a list of documents of an API Product
[**apiProductsApiProductIdDocumentsPost**](ApiProductDocumentsApi.md#apiProductsApiProductIdDocumentsPost) | **POST** /api-products/{apiProductId}/documents | Add a new document to an API Product


<a name="apiProductsApiProductIdDocumentsDocumentIdContentGet"></a>
# **apiProductsApiProductIdDocumentsDocumentIdContentGet**
> apiProductsApiProductIdDocumentsDocumentIdContentGet(apiProductId, documentId, accept, ifNoneMatch)

Get the content of an API Product document

This operation can be used to retrive the content of an API&#39;s document.  The document can be of 3 types. In each cases responses are different.  1. **Inline type**:    The content of the document will be retrieved in &#x60;text/plain&#x60; content type     _Sample cURL_ : &#x60;curl -k -H \&quot;Authorization:Bearer 579f0af4-37be-35c7-81a4-f1f1e9ee7c51\&quot; -F inlineContent&#x3D;@\&quot;docs.txt\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v1/apis/995a4972-3178-4b17-a374-756e0e19127c/documents/43c2bcce-60e7-405f-bc36-e39c0c5e189e/content&#x60; 2. **FILE type**:    The file will be downloaded with the related content type (eg. &#x60;application/pdf&#x60;) 3. **URL type**:     The client will recieve the URL of the document as the Location header with the response with - &#x60;303 See Other&#x60; 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String documentId = "documentId_example"; // String | Document Identifier 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apiProductsApiProductIdDocumentsDocumentIdContentGet(apiProductId, documentId, accept, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsDocumentIdContentGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **documentId** | **String**| Document Identifier  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdDocumentsDocumentIdContentPost"></a>
# **apiProductsApiProductIdDocumentsDocumentIdContentPost**
> DocumentDTO apiProductsApiProductIdDocumentsDocumentIdContentPost(apiProductId, documentId, file, inlineContent, ifMatch)

Upload the content of an API Product document

Thid operation can be used to upload a file or add inline content to an API Product document.  **IMPORTANT:** * Either **file** or **inlineContent** form data parameters should be specified at one time. * Document&#39;s source type should be **FILE** in order to upload a file to the document using **file** parameter. * Document&#39;s source type should be **INLINE** in order to add inline content to the document using **inlineContent** parameter. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String documentId = "documentId_example"; // String | Document Identifier 
File file = new File("/path/to/file.txt"); // File | Document to upload
String inlineContent = "inlineContent_example"; // String | Inline content of the document
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    DocumentDTO result = apiInstance.apiProductsApiProductIdDocumentsDocumentIdContentPost(apiProductId, documentId, file, inlineContent, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsDocumentIdContentPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **documentId** | **String**| Document Identifier  |
 **file** | **File**| Document to upload | [optional]
 **inlineContent** | **String**| Inline content of the document | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apiProductsApiProductIdDocumentsDocumentIdDelete"></a>
# **apiProductsApiProductIdDocumentsDocumentIdDelete**
> apiProductsApiProductIdDocumentsDocumentIdDelete(apiProductId, documentId, ifMatch)

Delete a document of an API Product

This operation can be used to delete a document associated with an API Product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String documentId = "documentId_example"; // String | Document Identifier 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apiProductsApiProductIdDocumentsDocumentIdDelete(apiProductId, documentId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsDocumentIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **documentId** | **String**| Document Identifier  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdDocumentsDocumentIdGet"></a>
# **apiProductsApiProductIdDocumentsDocumentIdGet**
> DocumentDTO apiProductsApiProductIdDocumentsDocumentIdGet(apiProductId, documentId, accept, ifNoneMatch)

Get a document of an API

This operation can be used to retrieve a particular document&#39;s metadata associated with an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String documentId = "documentId_example"; // String | Document Identifier 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    DocumentDTO result = apiInstance.apiProductsApiProductIdDocumentsDocumentIdGet(apiProductId, documentId, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsDocumentIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **documentId** | **String**| Document Identifier  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdDocumentsDocumentIdPut"></a>
# **apiProductsApiProductIdDocumentsDocumentIdPut**
> DocumentDTO apiProductsApiProductIdDocumentsDocumentIdPut(apiProductId, documentId, body, ifMatch)

Update a document of an API Product

This operation can be used to update metadata of an API&#39;s document. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
String documentId = "documentId_example"; // String | Document Identifier 
DocumentDTO body = new DocumentDTO(); // DocumentDTO | Document object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    DocumentDTO result = apiInstance.apiProductsApiProductIdDocumentsDocumentIdPut(apiProductId, documentId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsDocumentIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **documentId** | **String**| Document Identifier  |
 **body** | [**DocumentDTO**](DocumentDTO.md)| Document object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdDocumentsGet"></a>
# **apiProductsApiProductIdDocumentsGet**
> DocumentListDTO apiProductsApiProductIdDocumentsGet(apiProductId, limit, offset, accept, ifNoneMatch)

Get a list of documents of an API Product

This operation can be used to retrive a list of documents belonging to an API Product by providing the id of the API Product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    DocumentListDTO result = apiInstance.apiProductsApiProductIdDocumentsGet(apiProductId, limit, offset, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**DocumentListDTO**](DocumentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdDocumentsPost"></a>
# **apiProductsApiProductIdDocumentsPost**
> DocumentDTO apiProductsApiProductIdDocumentsPost(apiProductId, body)

Add a new document to an API Product

This operation can be used to add a new documentation to an API Product. This operation only adds the metadata of a document. To add the actual content we need to use **Upload the content of an API Product document ** API once we obtain a document Id by this operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiProductDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductDocumentsApi apiInstance = new ApiProductDocumentsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
DocumentDTO body = new DocumentDTO(); // DocumentDTO | Document object that needs to be added 
try {
    DocumentDTO result = apiInstance.apiProductsApiProductIdDocumentsPost(apiProductId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductDocumentsApi#apiProductsApiProductIdDocumentsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **body** | [**DocumentDTO**](DocumentDTO.md)| Document object that needs to be added  |

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

