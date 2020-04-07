# WorkflowsIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**workflowsExternalWorkflowRefGet**](WorkflowsIndividualApi.md#workflowsExternalWorkflowRefGet) | **GET** /workflows/{externalWorkflowRef} | Get details of a the pending workflow request according to the External Workflow Reference. 
[**workflowsUpdateWorkflowStatusPost**](WorkflowsIndividualApi.md#workflowsUpdateWorkflowStatusPost) | **POST** /workflows/update-workflow-status | Update workflow status


<a name="workflowsExternalWorkflowRefGet"></a>
# **workflowsExternalWorkflowRefGet**
> WorkflowInfoDTO workflowsExternalWorkflowRefGet(externalWorkflowRef, ifNoneMatch)

Get details of a the pending workflow request according to the External Workflow Reference. 

Using this operation, you can retrieve complete details of a pending workflow request that either belongs to application creation, application subscription, application registration, api state change, user self sign up.. You need to provide the External_Workflow_Reference of the workflow Request to retrive it. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.WorkflowsIndividualApi;


WorkflowsIndividualApi apiInstance = new WorkflowsIndividualApi();
String externalWorkflowRef = "externalWorkflowRef_example"; // String | from the externel workflow reference we decide what is the the pending request that the are requesting. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    WorkflowInfoDTO result = apiInstance.workflowsExternalWorkflowRefGet(externalWorkflowRef, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowsIndividualApi#workflowsExternalWorkflowRefGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **externalWorkflowRef** | **String**| from the externel workflow reference we decide what is the the pending request that the are requesting.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**WorkflowInfoDTO**](WorkflowInfoDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="workflowsUpdateWorkflowStatusPost"></a>
# **workflowsUpdateWorkflowStatusPost**
> WorkflowDTO workflowsUpdateWorkflowStatusPost(workflowReferenceId, body)

Update workflow status

This operation can be used to approve or reject a workflow task. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.WorkflowsIndividualApi;


WorkflowsIndividualApi apiInstance = new WorkflowsIndividualApi();
String workflowReferenceId = "workflowReferenceId_example"; // String | Workflow reference id 
WorkflowDTO body = new WorkflowDTO(); // WorkflowDTO | Workflow event that need to be updated 
try {
    WorkflowDTO result = apiInstance.workflowsUpdateWorkflowStatusPost(workflowReferenceId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowsIndividualApi#workflowsUpdateWorkflowStatusPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workflowReferenceId** | **String**| Workflow reference id  |
 **body** | [**WorkflowDTO**](WorkflowDTO.md)| Workflow event that need to be updated  |

### Return type

[**WorkflowDTO**](WorkflowDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

