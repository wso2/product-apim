# TagCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**tagsGet**](TagCollectionApi.md#tagsGet) | **GET** /tags | Get all tags 


<a name="tagsGet"></a>
# **tagsGet**
> TagList tagsGet(limit, offset, xWSO2Tenant, accept, ifNoneMatch)

Get all tags 

This operation can be used to retrieve a list of tags that are already added to APIs.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive tags that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s tags, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.TagCollectionApi;


TagCollectionApi apiInstance = new TagCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    TagList result = apiInstance.tagsGet(limit, offset, xWSO2Tenant, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TagCollectionApi#tagsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**TagList**](TagList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

