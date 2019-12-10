# ApiDocumentsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDocumentsDocumentIdContentGet**](ApiDocumentsApi.md#apisApiIdDocumentsDocumentIdContentGet) | **GET** /apis/{apiId}/documents/{documentId}/content | Get the content of an API document 
[**apisApiIdDocumentsGet**](ApiDocumentsApi.md#apisApiIdDocumentsGet) | **GET** /apis/{apiId}/documents | Get a list of documents of an API 


<a name="apisApiIdDocumentsDocumentIdContentGet"></a>
# **apisApiIdDocumentsDocumentIdContentGet**
> apisApiIdDocumentsDocumentIdContentGet(apiId, documentId, xWSO2Tenant, ifNoneMatch)

Get the content of an API document 

This operation can be used to retrive the content of an API&#39;s document.  The document can be of 3 types. In each cases responses are different.  1. **Inline type**:    The content of the document will be retrieved in &#x60;text/plain&#x60; content type 2. **FILE type**:    The file will be downloaded with the related content type (eg. &#x60;application/pdf&#x60;) 3. **URL type**:     The client will recieve the URL of the document as the Location header with the response with - &#x60;303 See Other&#x60;  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive the content of a document of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s document content, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    apiInstance.apisApiIdDocumentsDocumentIdContentGet(apiId, documentId, xWSO2Tenant, ifNoneMatch);
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdDocumentsGet"></a>
# **apisApiIdDocumentsGet**
> DocumentListDTO apisApiIdDocumentsGet(apiId, limit, offset, xWSO2Tenant, ifNoneMatch)

Get a list of documents of an API 

This operation can be used to retrive a list of documents belonging to an API by providing the id of the API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive documents of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s documents, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiDocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiDocumentsApi apiInstance = new ApiDocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    DocumentListDTO result = apiInstance.apisApiIdDocumentsGet(apiId, limit, offset, xWSO2Tenant, ifNoneMatch);
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**DocumentListDTO**](DocumentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

