
# AdvancedThrottlePolicyDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**policyId** | **String** | Id of policy |  [optional]
**policyName** | **String** | Name of policy | 
**displayName** | **String** | Display name of the policy |  [optional]
**description** | **String** | Description of the policy |  [optional]
**isDeployed** | **Boolean** | Indicates whether the policy is deployed successfully or not. |  [optional]
**defaultLimit** | [**ThrottleLimitDTO**](ThrottleLimitDTO.md) |  |  [optional]
**conditionalGroups** | [**List&lt;ConditionalGroupDTO&gt;**](ConditionalGroupDTO.md) | Group of conditions which allow adding different parameter conditions to the throttling limit.  |  [optional]



