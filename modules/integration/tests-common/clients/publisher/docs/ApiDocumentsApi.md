# ApiDocumentsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDocumentsDocumentIdContentGet**](ApiDocumentsApi.md#apisApiIdDocumentsDocumentIdContentGet) | **GET** /apis/{apiId}/documents/{documentId}/content | Get the content of an API document
[**apisApiIdDocumentsDocumentIdContentPost**](ApiDocumentsApi.md#apisApiIdDocumentsDocumentIdContentPost) | **POST** /apis/{apiId}/documents/{documentId}/content | Upload the content of an API document
[**apisApiIdDocumentsDocumentIdDelete**](ApiDocumentsApi.md#apisApiIdDocumentsDocumentIdDelete) | **DELETE** /apis/{apiId}/documents/{documentId} | Delete a document of an API
[**apisApiIdDocumentsDocumentIdGet**](ApiDocumentsApi.md#apisApiIdDocumentsDocumentIdGet) | **GET** /apis/{apiId}/documents/{documentId} | Get a document of an API
[**apisApiIdDocumentsDocumentIdPut**](ApiDocumentsApi.md#apisApiIdDocumentsDocumentIdPut) | **PUT** /apis/{apiId}/documents/{documentId} | Update a document of an API
[**apisApiIdDocumentsGet**](ApiDocumentsApi.md#apisApiIdDocumentsGet) | **GET** /apis/{apiId}/documents | Get a list of documents of an API
[**apisApiIdDocumentsPost**](ApiDocumentsApi.md#apisApiIdDocumentsPost) | **POST** /apis/{apiId}/documents | Add a new document to an API
[**validateDocument**](ApiDocumentsApi.md#validateDocument) | **POST** /apis/{apiId}/documents/validate | Check whether a document with the provided name exist.


<a name="apisApiIdDocumentsDocumentIdContentGet"></a>
# **apisApiIdDocumentsDocumentIdContentGet**
> apisApiIdDocumentsDocumentIdContentGet(apiId, documentId, ifNoneMatch)

Get the content of an API document

This operation can be used to retrive the content of an API&#39;s document.  The document can be of 3 types. In each cases responses are different.  1. **Inline type**:    The content of the document will be retrieved in &#x60;text/plain&#x60; content type     _Sample cURL_ : &#x60;curl -k -H \&quot;Authorization:Bearer 579f0af4-37be-35c7-81a4-f1f1e9ee7c51\&quot; -F inlineContent&#x3D;@\&quot;docs.txt\&quot; -X POST \&quot;https://localhost:9443/api/am/publisher/v1/apis/995a4972-3178-4b17-a374-756e0e19127c/documents/43c2bcce-60e7-405f-bc36-e39c0c5e189e/content&#x60; 2. **FILE type**:    The file will be downloaded with the related content type (eg. &#x60;application/pdf&#x60;) 3. **URL type**:     The client will recieve the URL of the document as the Location header with the response with - &#x60;303 See Other&#x60; 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apisApiIdDocumentsDocumentIdContentGet(apiId, documentId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsDocumentIdContentGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **documentId** | **String**| Document Identifier  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/octet-stream

<a name="apisApiIdDocumentsDocumentIdContentPost"></a>
# **apisApiIdDocumentsDocumentIdContentPost**
> DocumentDTO apisApiIdDocumentsDocumentIdContentPost(apiId, documentId, file, inlineContent, ifMatch)

Upload the content of an API document

Thid operation can be used to upload a file or add inline content to an API document.  **IMPORTANT:** * Either **file** or **inlineContent** form data parameters should be specified at one time. * Document&#39;s source type should be **FILE** in order to upload a file to the document using **file** parameter. * Document&#39;s source type should be **INLINE** in order to add inline content to the document using **inlineContent** parameter. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
File file = new File("/path/to/file.txt"); // File | Document to upload
String inlineContent = "inlineContent_example"; // String | Inline content of the document
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    DocumentDTO result = apiInstance.apisApiIdDocumentsDocumentIdContentPost(apiId, documentId, file, inlineContent, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsDocumentIdContentPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
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

<a name="apisApiIdDocumentsDocumentIdDelete"></a>
# **apisApiIdDocumentsDocumentIdDelete**
> apisApiIdDocumentsDocumentIdDelete(apiId, documentId, ifMatch)

Delete a document of an API

This operation can be used to delete a document associated with an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdDocumentsDocumentIdDelete(apiId, documentId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsDocumentIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **documentId** | **String**| Document Identifier  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdDocumentsDocumentIdGet"></a>
# **apisApiIdDocumentsDocumentIdGet**
> DocumentDTO apisApiIdDocumentsDocumentIdGet(apiId, documentId, ifNoneMatch)

Get a document of an API

This operation can be used to retrieve a particular document&#39;s metadata associated with an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    DocumentDTO result = apiInstance.apisApiIdDocumentsDocumentIdGet(apiId, documentId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsDocumentIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **documentId** | **String**| Document Identifier  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdDocumentsDocumentIdPut"></a>
# **apisApiIdDocumentsDocumentIdPut**
> DocumentDTO apisApiIdDocumentsDocumentIdPut(apiId, documentId, body, ifMatch)

Update a document of an API

This operation can be used to update metadata of an API&#39;s document. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
DocumentDTO body = new DocumentDTO(); // DocumentDTO | Document object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    DocumentDTO result = apiInstance.apisApiIdDocumentsDocumentIdPut(apiId, documentId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsDocumentIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
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

<a name="apisApiIdDocumentsGet"></a>
# **apisApiIdDocumentsGet**
> DocumentListDTO apisApiIdDocumentsGet(apiId, limit, offset, ifNoneMatch)

Get a list of documents of an API

This operation can be used to retrieve a list of documents belonging to an API by providing the id of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    DocumentListDTO result = apiInstance.apisApiIdDocumentsGet(apiId, limit, offset, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**DocumentListDTO**](DocumentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdDocumentsPost"></a>
# **apisApiIdDocumentsPost**
> DocumentDTO apisApiIdDocumentsPost(apiId, body, ifMatch)

Add a new document to an API

This operation can be used to add a new documentation to an API. This operation only adds the metadata of a document. To add the actual content we need to use **Upload the content of an API document ** API once we obtain a document Id by this operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
DocumentDTO body = new DocumentDTO(); // DocumentDTO | Document object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    DocumentDTO result = apiInstance.apisApiIdDocumentsPost(apiId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#apisApiIdDocumentsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**DocumentDTO**](DocumentDTO.md)| Document object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="validateDocument"></a>
# **validateDocument**
> DocumentDTO validateDocument(apiId, name, ifMatch)

Check whether a document with the provided name exist.

This operation can be used to verify the document name exists or not. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String name = "name_example"; // String | The name of the document which needs to be checked for the existance. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    DocumentDTO result = apiInstance.validateDocument(apiId, name, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiDocumentsApi#validateDocument");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **name** | **String**| The name of the document which needs to be checked for the existance.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

