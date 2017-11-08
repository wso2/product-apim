# BlacklistIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**throttlingBlacklistConditionIdDelete**](BlacklistIndividualApi.md#throttlingBlacklistConditionIdDelete) | **DELETE** /throttling/blacklist/{conditionId} | Delete a Blocking condition
[**throttlingBlacklistConditionIdGet**](BlacklistIndividualApi.md#throttlingBlacklistConditionIdGet) | **GET** /throttling/blacklist/{conditionId} | Get a Blocking Condition


<a name="throttlingBlacklistConditionIdDelete"></a>
# **throttlingBlacklistConditionIdDelete**
> throttlingBlacklistConditionIdDelete(conditionId, ifMatch, ifUnmodifiedSince)

Delete a Blocking condition

Deletes an existing Blocking condition 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.BlacklistIndividualApi;


BlacklistIndividualApi apiInstance = new BlacklistIndividualApi();
String conditionId = "conditionId_example"; // String | Blocking condition identifier  
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.throttlingBlacklistConditionIdDelete(conditionId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling BlacklistIndividualApi#throttlingBlacklistConditionIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **conditionId** | **String**| Blocking condition identifier   |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="throttlingBlacklistConditionIdGet"></a>
# **throttlingBlacklistConditionIdGet**
> BlockingCondition throttlingBlacklistConditionIdGet(conditionId, ifNoneMatch, ifModifiedSince)

Get a Blocking Condition

Retrieves a Blocking Condition providing the condition Id 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.BlacklistIndividualApi;


BlacklistIndividualApi apiInstance = new BlacklistIndividualApi();
String conditionId = "conditionId_example"; // String | Blocking condition identifier  
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    BlockingCondition result = apiInstance.throttlingBlacklistConditionIdGet(conditionId, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BlacklistIndividualApi#throttlingBlacklistConditionIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **conditionId** | **String**| Blocking condition identifier   |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**BlockingCondition**](BlockingCondition.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

