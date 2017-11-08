# ApplicationCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsGet**](ApplicationCollectionApi.md#applicationsGet) | **GET** /applications | Retrieve/Search applications 


<a name="applicationsGet"></a>
# **applicationsGet**
> ApplicationList applicationsGet(groupId, query, limit, offset, accept, ifNoneMatch)

Retrieve/Search applications 

This operation can be used to retrieve list of applications that is belonged to the user associated with the provided access token. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ApplicationCollectionApi;


ApplicationCollectionApi apiInstance = new ApplicationCollectionApi();
String groupId = "groupId_example"; // String | Application Group Id 
String query = "query_example"; // String | **Search condition**.  You can search for an application by specifying the name as \"query\" attribute.  Eg. \"app1\" will match an application if the name is exactly \"app1\".  Currently this does not support wildcards. Given name must exactly match the application name. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ApplicationList result = apiInstance.applicationsGet(groupId, query, limit, offset, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationCollectionApi#applicationsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **groupId** | **String**| Application Group Id  | [optional]
 **query** | **String**| **Search condition**.  You can search for an application by specifying the name as \&quot;query\&quot; attribute.  Eg. \&quot;app1\&quot; will match an application if the name is exactly \&quot;app1\&quot;.  Currently this does not support wildcards. Given name must exactly match the application name.  | [optional]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ApplicationList**](ApplicationList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

