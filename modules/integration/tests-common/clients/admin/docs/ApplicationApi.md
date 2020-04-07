# ApplicationApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdChangeOwnerPost**](ApplicationApi.md#applicationsApplicationIdChangeOwnerPost) | **POST** /applications/{applicationId}/change-owner | Change Application Owner


<a name="applicationsApplicationIdChangeOwnerPost"></a>
# **applicationsApplicationIdChangeOwnerPost**
> applicationsApplicationIdChangeOwnerPost(owner, applicationId)

Change Application Owner

This operation is used to change the owner of an Application. In order to change the owner of an application, we need to pass the new application owner as a query parameter 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApplicationApi;


ApplicationApi apiInstance = new ApplicationApi();
String owner = "owner_example"; // String | 
String applicationId = "applicationId_example"; // String | Application UUID 
try {
    apiInstance.applicationsApplicationIdChangeOwnerPost(owner, applicationId);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationApi#applicationsApplicationIdChangeOwnerPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **owner** | **String**|  |
 **applicationId** | **String**| Application UUID  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

