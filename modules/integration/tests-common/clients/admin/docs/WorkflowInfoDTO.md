
# WorkflowInfoDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**workflowType** | [**WorkflowTypeEnum**](#WorkflowTypeEnum) | Type of the Workflow Request. It shows which type of request is it.  |  [optional]
**workflowStatus** | [**WorkflowStatusEnum**](#WorkflowStatusEnum) | Show the Status of the the workflow request whether it is approved or created.  |  [optional]
**createdTime** | **String** | Time of the the workflow request created.  |  [optional]
**updatedTime** | **String** | Time of the the workflow request updated.  |  [optional]
**referenceId** | **String** | Workflow external reference is used to identify the workflow requests uniquely.  |  [optional]
**properties** | **Object** |  |  [optional]
**description** | **String** | description is a message with basic details about the workflow request.  |  [optional]


<a name="WorkflowTypeEnum"></a>
## Enum: WorkflowTypeEnum
Name | Value
---- | -----
APPLICATION_CREATION | &quot;AM_APPLICATION_CREATION&quot;
SUBSCRIPTION_CREATION | &quot;AM_SUBSCRIPTION_CREATION&quot;
USER_SIGNUP | &quot;AM_USER_SIGNUP&quot;
APPLICATION_REGISTRATION_PRODUCTION | &quot;AM_APPLICATION_REGISTRATION_PRODUCTION&quot;
APPLICATION_REGISTRATION_SANDBOX | &quot;AM_APPLICATION_REGISTRATION_SANDBOX&quot;
APPLICATION_DELETION | &quot;AM_APPLICATION_DELETION&quot;
API_STATE | &quot;AM_API_STATE&quot;
SUBSCRIPTION_DELETION | &quot;AM_SUBSCRIPTION_DELETION&quot;


<a name="WorkflowStatusEnum"></a>
## Enum: WorkflowStatusEnum
Name | Value
---- | -----
APPROVED | &quot;APPROVED&quot;
CREATED | &quot;CREATED&quot;



