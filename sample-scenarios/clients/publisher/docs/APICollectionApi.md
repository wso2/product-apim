# APICollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisGet**](APICollectionApi.md#apisGet) | **GET** /apis | Retrieve/Search APIs 
[**apisPost**](APICollectionApi.md#apisPost) | **POST** /apis | Create a new API


<a name="apisGet"></a>
# **apisGet**
> APIList apisGet(limit, offset, query, accept, ifNoneMatch)

Retrieve/Search APIs 

This operation provides you a list of available APIs qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API** operation. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APICollectionApi;


APICollectionApi apiInstance = new APICollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match an API if the provider of the API starts with \"wso2\".  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    APIList result = apiInstance.apisGet(limit, offset, query, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling APICollectionApi#apisGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**APIList**](APIList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisPost"></a>
# **apisPost**
> API apisPost(body, contentType)

Create a new API

This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APICollectionApi;


APICollectionApi apiInstance = new APICollectionApi();
API body = new API(); // API | API object that needs to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    API result = apiInstance.apisPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling APICollectionApi#apisPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**API**](API.md)| API object that needs to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**API**](API.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

