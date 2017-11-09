# BlacklistCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingBlacklistGet**](BlacklistCollectionApi.md#throttlingBlacklistGet) | **GET** /throttling/blacklist | Get all blocking condtions
[**throttlingBlacklistPost**](BlacklistCollectionApi.md#throttlingBlacklistPost) | **POST** /throttling/blacklist | Add a Blocking condition


<a name="throttlingBlacklistGet"></a>
# **throttlingBlacklistGet**
> BlockingConditionList throttlingBlacklistGet(accept, ifNoneMatch, ifModifiedSince)

Get all blocking condtions

Retrieves all existing blocking condtions. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.BlacklistCollectionApi;


BlacklistCollectionApi apiInstance = new BlacklistCollectionApi();
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    BlockingConditionList result = apiInstance.throttlingBlacklistGet(accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BlacklistCollectionApi#throttlingBlacklistGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**BlockingConditionList**](BlockingConditionList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingBlacklistPost"></a>
# **throttlingBlacklistPost**
> BlockingCondition throttlingBlacklistPost(body, contentType)

Add a Blocking condition

Adds a new Blocking condition. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.BlacklistCollectionApi;


BlacklistCollectionApi apiInstance = new BlacklistCollectionApi();
BlockingCondition body = new BlockingCondition(); // BlockingCondition | Blocking condition object that should to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    BlockingCondition result = apiInstance.throttlingBlacklistPost(body, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BlacklistCollectionApi#throttlingBlacklistPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**BlockingCondition**](BlockingCondition.md)| Blocking condition object that should to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**BlockingCondition**](BlockingCondition.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

