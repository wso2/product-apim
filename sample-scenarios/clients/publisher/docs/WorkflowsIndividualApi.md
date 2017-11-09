# WorkflowsIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**workflowsUpdateWorkflowStatusPost**](WorkflowsIndividualApi.md#workflowsUpdateWorkflowStatusPost) | **POST** /workflows/update-workflow-status | Update workflow status


<a name="workflowsUpdateWorkflowStatusPost"></a>
# **workflowsUpdateWorkflowStatusPost**
> Workflow workflowsUpdateWorkflowStatusPost(workflowReferenceId, body)

Update workflow status

This operation can be used to approve or reject a workflow task. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.WorkflowsIndividualApi;


WorkflowsIndividualApi apiInstance = new WorkflowsIndividualApi();
String workflowReferenceId = "workflowReferenceId_example"; // String | Workflow reference id 
Workflow body = new Workflow(); // Workflow | Workflow event that need to be updated 
try {
    Workflow result = apiInstance.workflowsUpdateWorkflowStatusPost(workflowReferenceId, body);
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
 **body** | [**Workflow**](Workflow.md)| Workflow event that need to be updated  |

### Return type

[**Workflow**](Workflow.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

