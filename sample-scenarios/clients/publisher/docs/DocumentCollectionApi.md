# DocumentCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDocumentsGet**](DocumentCollectionApi.md#apisApiIdDocumentsGet) | **GET** /apis/{apiId}/documents | Get a list of documents of an API
[**apisApiIdDocumentsPost**](DocumentCollectionApi.md#apisApiIdDocumentsPost) | **POST** /apis/{apiId}/documents | Add a new document to an API


<a name="apisApiIdDocumentsGet"></a>
# **apisApiIdDocumentsGet**
> DocumentList apisApiIdDocumentsGet(apiId, limit, offset, accept, ifNoneMatch)

Get a list of documents of an API

This operation can be used to retrive a list of documents belonging to an API by providing the id of the API. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.DocumentCollectionApi;


DocumentCollectionApi apiInstance = new DocumentCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    DocumentList result = apiInstance.apisApiIdDocumentsGet(apiId, limit, offset, accept, ifNoneMatch);
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
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**DocumentList**](DocumentList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdDocumentsPost"></a>
# **apisApiIdDocumentsPost**
> Document apisApiIdDocumentsPost(apiId, body, contentType)

Add a new document to an API

This operation can be used to add a new documentation to an API. This operation only adds the metadata of a document. To add the actual content we need to use **Upload the content of an API document ** API once we obtain a document Id by this operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.DocumentCollectionApi;


DocumentCollectionApi apiInstance = new DocumentCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
Document body = new Document(); // Document | Document object that needs to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    Document result = apiInstance.apisApiIdDocumentsPost(apiId, body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DocumentCollectionApi#apisApiIdDocumentsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **body** | [**Document**](Document.md)| Document object that needs to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**Document**](Document.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

