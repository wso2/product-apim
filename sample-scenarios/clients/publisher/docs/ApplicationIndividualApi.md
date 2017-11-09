# ApplicationIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdGet**](ApplicationIndividualApi.md#applicationsApplicationIdGet) | **GET** /applications/{applicationId} | Get details of an application


<a name="applicationsApplicationIdGet"></a>
# **applicationsApplicationIdGet**
> Application applicationsApplicationIdGet(applicationId, accept, ifNoneMatch, ifModifiedSince)

Get details of an application

This operation can be used to retrieve details of an individual application specifying the application id in the URI. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ApplicationIndividualApi;


ApplicationIndividualApi apiInstance = new ApplicationIndividualApi();
String applicationId = "applicationId_example"; // String | **Application Identifier** consisting of the UUID of the Application. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    Application result = apiInstance.applicationsApplicationIdGet(applicationId, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationIndividualApi#applicationsApplicationIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| **Application Identifier** consisting of the UUID of the Application.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**Application**](Application.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

