# DocumentCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDocumentsGet**](DocumentCollectionApi.md#apisApiIdDocumentsGet) | **GET** /apis/{apiId}/documents | Get a list of documents of an API 


<a name="apisApiIdDocumentsGet"></a>
# **apisApiIdDocumentsGet**
> DocumentList apisApiIdDocumentsGet(apiId, limit, offset, xWSO2Tenant, accept, ifNoneMatch)

Get a list of documents of an API 

This operation can be used to retrive a list of documents belonging to an API by providing the id of the API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive documents of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s documents, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.DocumentCollectionApi;


DocumentCollectionApi apiInstance = new DocumentCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    DocumentList result = apiInstance.apisApiIdDocumentsGet(apiId, limit, offset, xWSO2Tenant, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DocumentCollectionApi#apisApiIdDocumentsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**DocumentList**](DocumentList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

