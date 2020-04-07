# WorkflowCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**workflowsGet**](WorkflowCollectionApi.md#workflowsGet) | **GET** /workflows | Retrieve All pending workflow processes 


<a name="workflowsGet"></a>
# **workflowsGet**
> WorkflowListDTO workflowsGet(limit, offset, accept, ifNoneMatch, workflowType)

Retrieve All pending workflow processes 

This operation can be used to retrieve list of workflow pending processes. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.WorkflowCollectionApi;


WorkflowCollectionApi apiInstance = new WorkflowCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String workflowType = "workflowType_example"; // String | We need to show the values of each workflow process separately .for that we use workflow type. Workflow type can be AM_APPLICATION_CREATION, AM_SUBSCRIPTION_CREATION,   AM_USER_SIGNUP, AM_APPLICATION_REGISTRATION_PRODUCTION, AM_APPLICATION_REGISTRATION_SANDBOX. 
try {
    WorkflowListDTO result = apiInstance.workflowsGet(limit, offset, accept, ifNoneMatch, workflowType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowCollectionApi#workflowsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **workflowType** | **String**| We need to show the values of each workflow process separately .for that we use workflow type. Workflow type can be AM_APPLICATION_CREATION, AM_SUBSCRIPTION_CREATION,   AM_USER_SIGNUP, AM_APPLICATION_REGISTRATION_PRODUCTION, AM_APPLICATION_REGISTRATION_SANDBOX.  | [optional] [enum: AM_APPLICATION_CREATION, AM_SUBSCRIPTION_CREATION, AM_USER_SIGNUP, AM_APPLICATION_REGISTRATION_PRODUCTION, AM_APPLICATION_REGISTRATION_SANDBOX, AM_SUBSCRIPTION_DELETION, AM_APPLICATION_DELETION, AM_API_STATE]

### Return type

[**WorkflowListDTO**](WorkflowListDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

