# DocumentIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDocumentsDocumentIdContentGet**](DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdContentGet) | **GET** /apis/{apiId}/documents/{documentId}/content | Get the content of an API document 
[**apisApiIdDocumentsDocumentIdGet**](DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdGet) | **GET** /apis/{apiId}/documents/{documentId} | Get a document of an API 


<a name="apisApiIdDocumentsDocumentIdContentGet"></a>
# **apisApiIdDocumentsDocumentIdContentGet**
> apisApiIdDocumentsDocumentIdContentGet(apiId, documentId, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince)

Get the content of an API document 

This operation can be used to retrive the content of an API&#39;s document.  The document can be of 3 types. In each cases responses are different.  1. **Inline type**:    The content of the document will be retrieved in &#x60;text/plain&#x60; content type 2. **FILE type**:     The file will be downloaded with the related content type (eg. &#x60;application/pdf&#x60;) 3. **URL type**:     The client will recieve the URL of the document as the Location header with the response with - &#x60;303 See Other&#x60;  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive the content of a document of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s document content, you need to provide Authorization header.         

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.DocumentIndividualApi;


DocumentIndividualApi apiInstance = new DocumentIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String documentId = "documentId_example"; // String | Document Identifier 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
try {
    apiInstance.apisApiIdDocumentsDocumentIdContentGet(apiId, documentId, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling DocumentIndividualApi#apisApiIdDocumentsDocumentIdContentGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **documentId** | **String**| Document Identifier  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdDocumentsDocumentIdGet"></a>
# **apisApiIdDocumentsDocumentIdGet**
> Document apisApiIdDocumentsDocumentIdGet(apiId, documentId, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince)

Get a document of an API 

This operation can be used to retrieve a particular document&#39;s metadata associated with an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive a document of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s document, you need to provide Authorization header.         

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.DocumentIndividualApi;


DocumentIndividualApi apiInstance = new DocumentIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String documentId = "documentId_example"; // String | Document Identifier 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
try {
    Document result = apiInstance.apisApiIdDocumentsDocumentIdGet(apiId, documentId, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DocumentIndividualApi#apisApiIdDocumentsDocumentIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **documentId** | **String**| Document Identifier  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]

### Return type

[**Document**](Document.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

