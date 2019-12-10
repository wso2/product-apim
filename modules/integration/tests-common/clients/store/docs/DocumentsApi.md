# DocumentsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDocumentsDocumentIdGet**](DocumentsApi.md#apisApiIdDocumentsDocumentIdGet) | **GET** /apis/{apiId}/documents/{documentId} | Get a document of an API 


<a name="apisApiIdDocumentsDocumentIdGet"></a>
# **apisApiIdDocumentsDocumentIdGet**
> DocumentDTO apisApiIdDocumentsDocumentIdGet(apiId, documentId, xWSO2Tenant, ifNoneMatch)

Get a document of an API 

This operation can be used to retrieve a particular document&#39;s metadata associated with an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive a document of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s document, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.DocumentsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

DocumentsApi apiInstance = new DocumentsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String documentId = "documentId_example"; // String | Document Identifier 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    DocumentDTO result = apiInstance.apisApiIdDocumentsDocumentIdGet(apiId, documentId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DocumentsApi#apisApiIdDocumentsDocumentIdGet");
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

[**DocumentDTO**](DocumentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

